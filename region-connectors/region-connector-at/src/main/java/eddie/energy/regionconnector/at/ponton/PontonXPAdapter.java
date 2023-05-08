package eddie.energy.regionconnector.at.ponton;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.CMNotification;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.masterdata._01p30.MasterData;
import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.MessengerConnection;
import de.ponton.xp.adapter.api.domainvalues.*;
import de.ponton.xp.adapter.api.messages.InboundMessage;
import de.ponton.xp.adapter.api.messages.InboundMessageStatusUpdate;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import de.ponton.xp.adapter.api.messages.OutboundMessageStatusUpdate;
import eddie.energy.regionconnector.at.eda.ConsumptionRecordMapper;
import eddie.energy.regionconnector.at.eda.EdaAdapter;
import eddie.energy.regionconnector.at.eda.TransmissionException;
import eddie.energy.regionconnector.at.models.CMRequestStatus;
import eddie.energy.regionconnector.at.models.MessageCodes;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.io.*;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

class PontonXPAdapter implements EdaAdapter {
    final SubmissionPublisher<eddie.energy.regionconnector.api.v0.models.ConsumptionRecord> outboundDataStream = new SubmissionPublisher<>();
    final Sinks.Many<CMRequestStatus> cmStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    final Flow.Publisher<CMRequestStatus> cmStatusPublisher = JdkFlowAdapter.publisherToFlowPublisher(cmStatusSink.asFlux());
    private final Logger logger = LoggerFactory.getLogger(PontonXPAdapter.class);
    MessengerConnection messengerConnection;
    JAXBContext context = JAXBContext.newInstance(CMRequest.class, ConsumptionRecord.class, CMNotification.class, CMRevoke.class, MasterData.class);
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
            logger.info("Ponton XP adapter started.");
        } catch (de.ponton.xp.adapter.api.TransmissionException e) {
            throw new TransmissionException(e);
        }
    }

    private void outboundMessageStatusUpdateHandler(OutboundMessageStatusUpdate outboundMessageStatusUpdate) {
        var conversationId = outboundMessageStatusUpdate.getStatusMetaData().getConversationId().getValue();

        logger.info("Received status update for ConversationId: {} with result: {}", conversationId, outboundMessageStatusUpdate.getResult());
        switch (outboundMessageStatusUpdate.getResult()) {
            // I don't know if received is the right status here, as this is Just the ACK message from the receiver
            case SUCCESS -> {
                cmStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.RECEIVED, outboundMessageStatusUpdate.getDetailText(), conversationId));
            }
            case CONFIG_ERROR, CONTENT_ERROR, TRANSMISSION_ERROR, FAILED -> {
                cmStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.ERROR, outboundMessageStatusUpdate.getDetailText(), conversationId));
            }
            default -> {
                // Do nothing
            }
        }
    }

    private InboundMessageStatusUpdate inboundMessageHandler(InboundMessage inboundMessage) {
        var conversationId = inboundMessage.getInboundMetaData().getConversationId().getValue();
        var messageType = inboundMessage.getInboundMetaData().getMessageType().getName().getValue();
        var messageVersion = inboundMessage.getInboundMetaData().getMessageType().getVersion().getValue();
        logger.info("Received message type: {} (version {}) for ConversationId: {}", messageType, messageVersion, inboundMessage.getInboundMetaData().getConversationId().getValue());

        return switch (messageType) {
            case MessageCodes.Notification.ANSWER ->
                    handleCMNotificationMessage(inboundMessage, new CMRequestStatus(CMRequestStatus.Status.DELIVERED, "CCMO request has been delivered.", conversationId));
            case MessageCodes.Notification.ACCEPT ->
                    handleCMNotificationMessage(inboundMessage, new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "CCMO request has been accepted.", conversationId));
            case MessageCodes.Notification.REJECT ->
                    handleCMNotificationMessage(inboundMessage, new CMRequestStatus(CMRequestStatus.Status.REJECTED, "CCMO request has been rejected.", conversationId));
            case MessageCodes.MASTER_DATA -> handleMasterDataMessage(inboundMessage);
            case MessageCodes.CONSUMPTION_RECORD -> handleConsumptionRecordMessage(inboundMessage);
            case MessageCodes.Revoke.CUSTOMER, MessageCodes.Revoke.IMPLICIT -> handleRevokeMessage(inboundMessage);
            default -> {
                logger.warn("Received message type {} (version {}) is not supported.", messageType, messageVersion);
                yield InboundMessageStatusUpdate.newBuilder()
                        .setInboundMessage(inboundMessage)
                        .setStatus(InboundStatusEnum.REJECTED)
                        .setStatusText("message type " + messageType + " version " + messageVersion + " is not supported.")
                        .build();
            }
        };
    }

    private InboundMessageStatusUpdate handleCMNotificationMessage(InboundMessage inboundMessage, CMRequestStatus status) {
        cmStatusSink.tryEmitNext(status);
        logger.info("Received CMNotification update: {} for request {}", status.status(), status.conversationId());
        return InboundMessageStatusUpdate.newBuilder()
                .setInboundMessage(inboundMessage)
                .setStatus(InboundStatusEnum.SUCCESS)
                .setStatusText("CMNotification processed")
                .build();
    }

    private InboundMessageStatusUpdate handleMasterDataMessage(InboundMessage inboundMessage) {
        // send consumption record to OutboundDataStreamConnector
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var masterData = (MasterData) unmarshaller.unmarshal(inputStream);
            // TODO: handle master data
            logger.info("Received master data: {}", ReflectionToStringBuilder.toString(masterData, ToStringStyle.NO_CLASS_NAME_STYLE));

            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("Master data successfully delivered to backend.")
                    .build();
        } catch (JAXBException e) {
            logger.error("Error while trying to unmarshal MasterData of schema-version {}", inboundMessage.getInboundMetaData().getMessageType().getVersion().getValue(), e);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.REJECTED)
                    .setStatusText(e.getMessage())
                    .build();
        } catch (IOException e) {
            logger.error("Error while reading master data message", e);
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
            // the process is documented here https://www.ebutilities.at/prozesse/230
            // we might have to create a ABLEHNUNG_CRMSG (CPNotification) if the message was not valid, see https://www.ebutilities.at/prozesse/230/marktnachrichten/615
            outboundDataStream.submit(outboundConsumptionRecord);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("ConsumptionRecord successfully delivered to backend.")
                    .build();
        } catch (JAXBException e) {
            logger.error("Error while trying to unmarshal ConsumptionRecord of schema-version {}", inboundMessage.getInboundMetaData().getMessageType().getVersion().getValue(), e);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.REJECTED)
                    .setStatusText(e.getMessage())
                    .build();
        } catch (IOException e) {
            logger.error("Error while reading ConsumptionRecord message", e);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.TEMPORARY_ERROR)
                    .setStatusText(e.getMessage())
                    .build();
        }
    }

    private InboundMessageStatusUpdate handleRevokeMessage(InboundMessage inboundMessage) {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            // convert stream to consumption record
            var cmRevoke = (CMRevoke) unmarshaller.unmarshal(inputStream);

            // TODO inform framework, that consumption record has been denied
            logger.info("Received revoke message for ConversationID: {}", cmRevoke.getProcessDirectory().getConversationId());

            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("CMRevoke successfully delivered to backend.")
                    .build();

        } catch (JAXBException e) {
            logger.error("Error while trying to unmarshal CMRevoke of schema-version {}", inboundMessage.getInboundMetaData().getMessageType().getVersion().getValue(), e);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.REJECTED)
                    .setStatusText(e.getMessage())
                    .build();
        } catch (IOException e) {
            logger.error("Error while reading CMRevoke message", e);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.TEMPORARY_ERROR)
                    .setStatusText(e.getMessage())
                    .build();
        }
    }

    @Override
    public void sendCMRequest(CMRequest request) throws TransmissionException, JAXBException {
        // convert request to XML
        var outputStream = new ByteArrayOutputStream();
        marshaller.marshal(request, outputStream);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        final OutboundMetaData outboundMetaData = OutboundMetaData.newBuilder()
                .setSenderId(new SenderId(request.getMarketParticipantDirectory().getRoutingHeader().getSender().getMessageAddress()))
                .setReceiverId(new ReceiverId(request.getMarketParticipantDirectory().getRoutingHeader().getReceiver().getMessageAddress()))
                .setMessageType(new MessageType.MessageTypeBuilder()
                        .setSchemaSet(new SchemaSet(MessageCodes.Request.SCHEMA))
                        .setVersion(new MessageTypeVersion(MessageCodes.Request.VERSION))
                        .setName(new MessageTypeName(MessageCodes.Request.CODE))
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

        cmStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.SENT, "CCMO request has been sent", request.getProcessDirectory().getConversationId()));
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