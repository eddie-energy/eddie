package eddie.energy.regionconnector.at.ponton;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.CMNotification;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.ConsumptionRecord;
import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.MessengerConnection;
import de.ponton.xp.adapter.api.domainvalues.*;
import de.ponton.xp.adapter.api.messages.InboundMessage;
import de.ponton.xp.adapter.api.messages.InboundMessageStatusUpdate;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import de.ponton.xp.adapter.api.messages.OutboundMessageStatusUpdate;
import eddie.energy.regionconnector.at.eda.CRMessageCodes;
import eddie.energy.regionconnector.at.eda.ConsumptionRecordMapper;
import eddie.energy.regionconnector.at.eda.EdaAdapter;
import eddie.energy.regionconnector.at.eda.TransmissionException;
import eddie.energy.regionconnector.at.models.CCMOMessageCodes;
import eddie.energy.regionconnector.at.models.CCMORequest;
import eddie.energy.regionconnector.at.models.CMRequestStatus;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

class PontonXPAdapter implements EdaAdapter {
    final SubmissionPublisher<eddie.energy.regionconnector.api.v0.models.ConsumptionRecord> outboundDataStream = new SubmissionPublisher<>();
    final Sinks.Many<CMRequestStatus> cmStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    final Flow.Publisher<CMRequestStatus> cmStatusPublisher = JdkFlowAdapter.publisherToFlowPublisher(cmStatusSink.asFlux());
    final Map<ConversationId, String> conversationIdCMRequestIdMap = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(PontonXPAdapter.class);
    MessengerConnection messengerConnection;
    JAXBContext context = JAXBContext.newInstance(CMRequest.class, ConsumptionRecord.class, CMNotification.class);
    Marshaller marshaller = context.createMarshaller();
    Unmarshaller unmarshaller = context.createUnmarshaller();

    PontonXPAdapter(PontonXPAdapterConfig config) throws IOException, ConnectionException, JAXBException {
        final String adapterId = config.getAdapterId();
        final String adapterVersion = config.getAdapterVersion();
        final String hostname = config.getHostname();
        final int port = config.getPort();
        final File workFolder = new File(config.getWorkFolder());

        if (!workFolder.exists()) {
            throw new IOException("Work folder does not exist: " + workFolder.getAbsolutePath());
        }

        final AdapterInfo adapterInfo = AdapterInfo.newBuilder()
                .setAdapterId(adapterId)
                .setAdapterVersion(adapterVersion)
                .build();

        this.messengerConnection = MessengerConnection.newBuilder()
                .setWorkFolder(workFolder)
                .setAdapterInfo(adapterInfo)
                .addMessengerInstance(MessengerInstance.create(hostname, port))
                .onMessageReceive(this::inboundMessageHandler)
                .onMessageStatusUpdate(this::outboundMessageStatusUpdateHandler)
                .onAdapterStatusRequest(() -> adapterId + " " + adapterVersion + " is running.")
                .build();
    }

    @Override
    public void start() throws TransmissionException {
        try {
            messengerConnection.start();
        } catch (de.ponton.xp.adapter.api.TransmissionException e) {
            throw new TransmissionException(e);
        }
    }

    private void outboundMessageStatusUpdateHandler(OutboundMessageStatusUpdate outboundMessageStatusUpdate) {
        var conversationId = outboundMessageStatusUpdate.getStatusMetaData().getConversationId();
        var requestId = conversationIdCMRequestIdMap.get(conversationId);
        if (requestId == null) {
            logger.warn("Received status update for unknown conversation id: {}", conversationId);
            return;
        }

        switch (outboundMessageStatusUpdate.getResult()) {
            // I don't know if received is the right status here, as this is Just the ACK message from the receiver
            case SUCCESS -> {
                cmStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.RECEIVED, outboundMessageStatusUpdate.getDetailText(), requestId));
                conversationIdCMRequestIdMap.remove(conversationId);
            }
            case CONFIG_ERROR, CONTENT_ERROR, TRANSMISSION_ERROR, FAILED -> {
                cmStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.ERROR, outboundMessageStatusUpdate.getDetailText(), requestId));
                conversationIdCMRequestIdMap.remove(conversationId);
            }
            default -> logger.info("Unhandled status update: {}", outboundMessageStatusUpdate);
        }
    }

    private InboundMessageStatusUpdate inboundMessageHandler(InboundMessage inboundMessage) {
        var messageType = inboundMessage.getInboundMetaData().getMessageType();
        return switch (messageType.getName().getValue()) {
            case CCMOMessageCodes.ANTWORT_CCMO ->
                    handleCMNotificationMessage(inboundMessage, new CMRequestStatus(CMRequestStatus.Status.DELIVERED, "CCMO request has been delivered."));
            case CCMOMessageCodes.ZUSTIMMUNG_CCMO ->
                    handleCMNotificationMessage(inboundMessage, new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "CCMO request has been accepted."));
            case CCMOMessageCodes.ABLEHNUNG_CCMO ->
                    handleCMNotificationMessage(inboundMessage, new CMRequestStatus(CMRequestStatus.Status.REJECTED, "CCMO request has been rejected."));
            case CRMessageCodes.DATEN_CRMSG -> handleConsumptionRecordMessage(inboundMessage);
            case CRMessageCodes.ABLEHNUNG_CRMSG -> handleConsumptionRecordDenial(inboundMessage);
            default -> InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.REJECTED)
                    .setStatusText("message type " + messageType.getName() + " is not supported.")
                    .build();
        };
    }

    private InboundMessageStatusUpdate handleCMNotificationMessage(InboundMessage inboundMessage, CMRequestStatus status) {
        // send consumption record to OutboundDataStreamConnector
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            // convert stream to consumption record
            var cmNotification = (CMNotification) unmarshaller.unmarshal(inputStream);
            status.setRequestId(cmNotification.getProcessDirectory().getCMRequestId());
            cmStatusSink.tryEmitNext(status);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("CMNotification processed")
                    .build();
        } catch (Exception e) {
            logger.error("Error while handling consumption record message", e);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.TEMPORARY_ERROR)
                    .setStatusText(e.getMessage())
                    .build();
        }
    }


    private InboundMessageStatusUpdate handleConsumptionRecordMessage(InboundMessage inboundMessage) {
        // send consumption record to OutboundDataStreamConnector
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            // convert stream to consumption record
            var consumptionRecord = (ConsumptionRecord) unmarshaller.unmarshal(inputStream);
            var outboundConsumptionRecord = ConsumptionRecordMapper.mapToCIM(consumptionRecord);
            outboundDataStream.submit(outboundConsumptionRecord);

            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("ConsumptionRecord successfully delivered to backend.")
                    .build();

        } catch (Exception e) {
            logger.error("Error while handling consumption record message", e);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.TEMPORARY_ERROR)
                    .setStatusText(e.getMessage())
                    .build();
        }
    }

    private InboundMessageStatusUpdate handleConsumptionRecordDenial(InboundMessage inboundMessage) {
        // inform framework, that consumption record has been denied
        return InboundMessageStatusUpdate.newBuilder()
                .setInboundMessage(inboundMessage)
                .setStatus(InboundStatusEnum.SUCCESS)
                .setStatusText("message successfully delivered to backend.")
                .build();
    }

    @Override
    public void sendCMRequest(CCMORequest request) throws TransmissionException, JAXBException {
        var cmRequest = request.toCMRequest();

        // convert request to XML
        var outputStream = new ByteArrayOutputStream();
        marshaller.marshal(cmRequest, outputStream);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        final OutboundMetaData outboundMetaData = OutboundMetaData.newBuilder()
                .setSenderId(new SenderId(request.getSender()))
                .setReceiverId(new ReceiverId(request.getReceiver()))
                .setMessageType(new MessageType.MessageTypeBuilder()
                        .setSchemaSet(new SchemaSet("CM_REQ_ONL_01.10"))
                        .setVersion(new MessageTypeVersion("01.10"))
                        .setName(new MessageTypeName(CCMOMessageCodes.ANFORDERUNG_CCMO))
                        .setMimeType(new MimeType("text/xml"))
                        .build())
                .build();

        final OutboundMessage outboundMessage = OutboundMessage.newBuilder()
                .setInputStream(inputStream)
                .setOutboundMetaData(outboundMetaData)
                .build();

        try {
            messengerConnection.sendMessage(outboundMessage);
        } catch (de.ponton.xp.adapter.api.TransmissionException e) {
            throw new TransmissionException(e);
        }

        conversationIdCMRequestIdMap.put(outboundMessage.getOutboundMetaData().getConversationId(), cmRequest.getProcessDirectory().getCMRequestId());
        cmStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.SENT, "CCMO request has been sent", cmRequest.getProcessDirectory().getCMRequestId()));
    }

    @Override
    public void close() {
        cmStatusSink.tryEmitComplete();
        messengerConnection.close();
    }

    @Override
    public void subscribeToConsumptionRecordPublisher(Flow.Subscriber<eddie.energy.regionconnector.api.v0.models.ConsumptionRecord> subscriber) {
        outboundDataStream.subscribe(subscriber);
    }

    @Override
    public void subscribeToCMRequestStatusPublisher(Flow.Subscriber<CMRequestStatus> subscriber) {
        cmStatusPublisher.subscribe(subscriber);
    }

}