package energy.eddie.regionconnector.at.eda.ponton;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.CMNotification;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.masterdata._01p30.MasterData;
import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.MessengerConnection;
import de.ponton.xp.adapter.api.domainvalues.*;
import de.ponton.xp.adapter.api.messages.InboundMessage;
import de.ponton.xp.adapter.api.messages.InboundMessageStatusUpdate;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import de.ponton.xp.adapter.api.messages.OutboundMessageStatusUpdate;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.*;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;


public class PontonXPAdapter implements EdaAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PontonXPAdapter.class);
    private static final String PONTON_HOST = "pontonHost";
    private static final int PING_TIMEOUT = 2000;
    private final Sinks.Many<CMRequestStatus> requestStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<CMRevoke> cmRevokeSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<MasterData> masterDataSink = Sinks.many().multicast().onBackpressureBuffer();
    private final MessengerConnection messengerConnection;
    private final JAXBContext context = JAXBContext.newInstance(CMRequest.class, ConsumptionRecord.class, CMNotification.class, CMRevoke.class, MasterData.class, CMRevoke.class);
    private final Marshaller marshaller = context.createMarshaller();
    private final Unmarshaller unmarshaller = context.createUnmarshaller();
    private final PontonXPAdapterConfiguration config;

    public PontonXPAdapter(PontonXPAdapterConfiguration config) throws IOException, ConnectionException, JAXBException {
        this.config = config;
        final String adapterId = config.adapterId();
        final String adapterVersion = config.adapterVersion();
        final String hostname = config.hostname();
        final int port = config.port();
        final File workFolder = new File(config.workFolder());

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
            LOGGER.info("Ponton XP adapter started.");
        } catch (de.ponton.xp.adapter.api.TransmissionException e) {
            throw new TransmissionException(e);
        }
    }

    private void outboundMessageStatusUpdateHandler(OutboundMessageStatusUpdate outboundMessageStatusUpdate) {
        var conversationId = outboundMessageStatusUpdate.getStatusMetaData().getConversationId().getValue();

        LOGGER.info("Received status update for ConversationId: '{}' with result: '{}'", conversationId, outboundMessageStatusUpdate.getResult());
        switch (outboundMessageStatusUpdate.getResult()) {
            case SUCCESS ->  // success just indicates that the message was received by the other party (dso)
                    requestStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.RECEIVED, outboundMessageStatusUpdate.getDetailText(), conversationId));
            case CONFIG_ERROR, CONTENT_ERROR, TRANSMISSION_ERROR, FAILED ->
                    requestStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.ERROR, outboundMessageStatusUpdate.getDetailText(), conversationId));
            default -> {
                // Ignore the other status updates
            }
        }
    }

    private InboundMessageStatusUpdate inboundMessageHandler(InboundMessage inboundMessage) {
        var messageType = inboundMessage.getInboundMetaData().getMessageType().getName().getValue();
        var messageVersion = inboundMessage.getInboundMetaData().getMessageType().getVersion().getValue();
        LOGGER.info("Received message type: '{}' (version '{}') with ConversationId: '{}'", messageType, messageVersion, inboundMessage.getInboundMetaData().getConversationId().getValue());

        try {
            return switch (messageType) {
                case MessageCodes.Notification.ANSWER -> handleCMNotificationMessage(inboundMessage);
                case MessageCodes.Notification.ACCEPT -> handleCMAcceptNotificationMessage(inboundMessage);
                case MessageCodes.Notification.REJECT -> handleCMRejectNotificationMessage(inboundMessage);
                case MessageCodes.MASTER_DATA -> handleMasterDataMessage(inboundMessage);
                case MessageCodes.CONSUMPTION_RECORD -> handleConsumptionRecordMessage(inboundMessage);
                case MessageCodes.Revoke.CUSTOMER, MessageCodes.Revoke.IMPLICIT -> handleRevokeMessage(inboundMessage);
                default -> {
                    LOGGER.warn("Received message type '{}' (version '{}') is not supported.", messageType, messageVersion);
                    yield InboundMessageStatusUpdate.newBuilder()
                            .setInboundMessage(inboundMessage)
                            .setStatus(InboundStatusEnum.REJECTED)
                            .setStatusText("message type " + messageType + " version " + messageVersion + " is not supported.")
                            .build();
                }
            };
        } catch (JAXBException e) {
            LOGGER.error("Error while trying to unmarshal contents of message type '{}' in schema-version '{}'", messageType, messageVersion, e);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.REJECTED)
                    .setStatusText(e.getMessage())
                    .build();
        } catch (IOException e) {
            LOGGER.error("Error while reading input stream of message", e);
            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.TEMPORARY_ERROR)
                    .setStatusText(e.getMessage())
                    .build();
        }
    }

    private InboundMessageStatusUpdate handleCMNotificationMessage(InboundMessage inboundMessage) throws JAXBException, IOException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var notification = (CMNotification) unmarshaller.unmarshal(inputStream);
            var cmRequestId = notification.getProcessDirectory().getCMRequestId();
            var meteringPoint = notification.getProcessDirectory().getResponseData().get(0).getMeteringPoint();

            var status = new CMRequestStatus(CMRequestStatus.Status.DELIVERED, "CCMO request has been delivered.", notification.getProcessDirectory().getConversationId());
            status.setCmRequestId(cmRequestId);
            status.setMeteringPoint(meteringPoint);

            requestStatusSink.tryEmitNext(status);
            LOGGER.info("Received CMNotification '{}' for CMRequestId '{}' with ConversationId '{}'", status.getStatus(), cmRequestId, status.getConversationId());

            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("CMNotification processed")
                    .build();
        }
    }

    private InboundMessageStatusUpdate handleCMAcceptNotificationMessage(InboundMessage inboundMessage) throws IOException, JAXBException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var notification = (CMNotification) unmarshaller.unmarshal(inputStream);
            var consentId = notification.getProcessDirectory().getResponseData().get(0).getConsentId();
            var cmRequestId = notification.getProcessDirectory().getCMRequestId();
            var meteringPoint = notification.getProcessDirectory().getResponseData().get(0).getMeteringPoint();

            var status = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "CCMO request has been accepted.", notification.getProcessDirectory().getConversationId());
            status.setCmConsentId(consentId);
            status.setCmRequestId(cmRequestId);
            status.setMeteringPoint(meteringPoint);

            requestStatusSink.tryEmitNext(status);
            LOGGER.info("Received CMNotification: ACCEPTED for CMRequestId '{}' with ConversationId '{}' and ConsentId: '{}'", cmRequestId, status.getConversationId(), consentId);

            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("CMNotification processed")
                    .build();
        }
    }

    private InboundMessageStatusUpdate handleCMRejectNotificationMessage(InboundMessage inboundMessage) throws IOException, JAXBException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var notification = (CMNotification) unmarshaller.unmarshal(inputStream);
            var responseCode = notification.getProcessDirectory().getResponseData().get(0).getResponseCode().get(0);
            var cmRequestId = notification.getProcessDirectory().getCMRequestId();
            var meteringPoint = notification.getProcessDirectory().getResponseData().get(0).getMeteringPoint();

            // maybe turn this into an enum that handles the mapping
            var reason = switch (responseCode) {
                case 56 -> "Metering point not found";
                case 178 -> "Consent already exists";
                case 174 -> "Requested data not deliverable";
                case 173 -> "Time-out";
                case 172 -> "Customer rejected the request";
                case 82 -> "Invalid dates";
                case 76 -> "Invalid request data";
                case 57 -> "Metering point not supplied";
                case 179 -> "ConsentId already exists";
                default -> responseCode + " - Unknown response code";
            };

            var status = new CMRequestStatus(CMRequestStatus.Status.REJECTED, reason, notification.getProcessDirectory().getConversationId());
            status.setCmRequestId(cmRequestId);
            status.setMeteringPoint(meteringPoint);

            requestStatusSink.tryEmitNext(status);
            LOGGER.info("Received CMNotification: REJECTED for CMRequestId '{}' with  ConversationId '{}', reason '{}'", cmRequestId, status.getConversationId(), reason);

            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("CMNotification processed")
                    .build();
        }
    }


    private InboundMessageStatusUpdate handleMasterDataMessage(InboundMessage inboundMessage) throws IOException, JAXBException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var masterData = (MasterData) unmarshaller.unmarshal(inputStream);
            masterDataSink.tryEmitNext(masterData);

            LOGGER.info("Received master data for MeteringPoint '{}' with ConversationId '{}'", masterData.getProcessDirectory().getMeteringPoint(), masterData.getProcessDirectory().getConversationId());

            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("Master data successfully delivered to backend.")
                    .build();
        }
    }

    private InboundMessageStatusUpdate handleConsumptionRecordMessage(InboundMessage inboundMessage) throws IOException, JAXBException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var consumptionRecord = (ConsumptionRecord) unmarshaller.unmarshal(inputStream);
            // the process is documented here https://www.ebutilities.at/prozesse/230
            // we might have to create a ABLEHNUNG_CRMSG (CPNotification) if the message was not valid, see https://www.ebutilities.at/prozesse/230/marktnachrichten/615
            consumptionRecordSink.tryEmitNext(consumptionRecord);
            LOGGER.info("Received consumption record for MeteringPoint '{}' with ConversationId '{}'", consumptionRecord.getProcessDirectory().getMeteringPoint(), consumptionRecord.getProcessDirectory().getConversationId());

            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("ConsumptionRecord successfully delivered to backend.")
                    .build();
        }
    }

    private InboundMessageStatusUpdate handleRevokeMessage(InboundMessage inboundMessage) throws IOException, JAXBException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            // convert stream to consumption record
            var cmRevoke = (CMRevoke) unmarshaller.unmarshal(inputStream);

            cmRevokeSink.tryEmitNext(cmRevoke);
            LOGGER.info("Received revoke message for MeteringPoint '{}' of ConsentId '{}' with ConversationId '{}'", cmRevoke.getProcessDirectory().getConsentId(), cmRevoke.getProcessDirectory().getMeteringPoint(), cmRevoke.getProcessDirectory().getConversationId());

            return InboundMessageStatusUpdate.newBuilder()
                    .setInboundMessage(inboundMessage)
                    .setStatus(InboundStatusEnum.SUCCESS)
                    .setStatusText("CMRevoke successfully delivered to backend.")
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
            LOGGER.info("Sending CCMO request to DSO '{}' with RequestID '{}'", request.getMarketParticipantDirectory().getRoutingHeader().getReceiver().getMessageAddress(), request.getProcessDirectory().getCMRequestId());
            messengerConnection.sendMessage(outboundMessage);
        } catch (de.ponton.xp.adapter.api.TransmissionException e) {
            LOGGER.error("Error sending CCMO request to DSO '{}'", request.getMarketParticipantDirectory().getRoutingHeader().getReceiver().getMessageAddress(), e);
            throw new TransmissionException(e);
        }

        requestStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.SENT, "CCMO request has been sent", request.getProcessDirectory().getConversationId()));
    }


    @Override
    public void sendCMRevoke(CMRevoke revoke) throws TransmissionException, JAXBException {
        // convert revoke to XML
        var outputStream = new ByteArrayOutputStream();
        marshaller.marshal(revoke, outputStream);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        final OutboundMetaData outboundMetaData = OutboundMetaData.newBuilder()
                .setSenderId(new SenderId(revoke.getMarketParticipantDirectory().getRoutingHeader().getSender().getMessageAddress()))
                .setReceiverId(new ReceiverId(revoke.getMarketParticipantDirectory().getRoutingHeader().getReceiver().getMessageAddress()))
                .setMessageType(new MessageType.MessageTypeBuilder()
                        .setSchemaSet(new SchemaSet(MessageCodes.Revoke.EligibleParty.SCHEMA))
                        .setVersion(new MessageTypeVersion(MessageCodes.Revoke.VERSION))
                        .setName(new MessageTypeName(MessageCodes.Revoke.EligibleParty.REVOKE))
                        .setMimeType(new MimeType("text/xml"))
                        .build())
                .build();

        final OutboundMessage outboundMessage = OutboundMessage.newBuilder()
                .setInputStream(inputStream)
                .setOutboundMetaData(outboundMetaData)
                .build();

        try {
            LOGGER.info("Sending CMRevoke to DSO '{}' with ConsentId '{}'", revoke.getMarketParticipantDirectory().getRoutingHeader().getReceiver().getMessageAddress(), revoke.getProcessDirectory().getConsentId());
            messengerConnection.sendMessage(outboundMessage);
        } catch (de.ponton.xp.adapter.api.TransmissionException e) {
            LOGGER.error("Error sending CMRevoke to DSO '{}'", revoke.getMarketParticipantDirectory().getRoutingHeader().getReceiver().getMessageAddress(), e);
            throw new TransmissionException(e);
        }
    }

    @Override
    public Map<String, HealthState> health() {
        Map<String, HealthState> healthChecks = new HashMap<>();
        try {
            InetAddress address = InetAddress.getByName(config.hostname());
            healthChecks.put(PONTON_HOST, address.isReachable(PING_TIMEOUT) ? HealthState.UP : HealthState.DOWN);
        } catch (IOException e) {
            LOGGER.warn("Ponton  XP Messenger Host not reachable", e);
            healthChecks.put(PONTON_HOST, HealthState.DOWN);
        }
        return healthChecks;
    }

    @Override
    public void close() {
        requestStatusSink.tryEmitComplete();
        consumptionRecordSink.tryEmitComplete();
        masterDataSink.tryEmitComplete();
        cmRevokeSink.tryEmitComplete();
        messengerConnection.close();
    }

    @Override
    public Flux<CMRequestStatus> getCMRequestStatusStream() {
        return requestStatusSink.asFlux();
    }

    @Override
    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        return consumptionRecordSink.asFlux();
    }

    @Override
    public Flux<CMRevoke> getCMRevokeStream() {
        return cmRevokeSink.asFlux();
    }

    @Override
    public Flux<MasterData> getMasterDataStream() {
        return masterDataSink.asFlux();
    }
}