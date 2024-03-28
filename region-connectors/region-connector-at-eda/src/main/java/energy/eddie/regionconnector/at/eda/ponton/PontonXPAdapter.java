package energy.eddie.regionconnector.at.eda.ponton;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.CMNotification;
import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ResponseDataType;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.masterdata._01p30.MasterData;
import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.MessengerConnection;
import de.ponton.xp.adapter.api.domainvalues.AdapterInfo;
import de.ponton.xp.adapter.api.domainvalues.InboundStatusEnum;
import de.ponton.xp.adapter.api.domainvalues.MessengerInstance;
import de.ponton.xp.adapter.api.messages.InboundMessage;
import de.ponton.xp.adapter.api.messages.InboundMessageStatusUpdate;
import de.ponton.xp.adapter.api.messages.OutboundMessageStatusUpdate;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PontonXPAdapter implements EdaAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PontonXPAdapter.class);
    private static final String PONTON_HOST = "pontonHost";
    private static final int PING_TIMEOUT = 2000;
    private static final String CM_NOTIFICATION_PROCESSED = "CMNotification processed";
    private final Sinks.Many<CMRequestStatus> requestStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().unicast().onBackpressureBuffer();
    private final Sinks.Many<CMRevoke> cmRevokeSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<MasterData> masterDataSink = Sinks.many().multicast().onBackpressureBuffer();
    private final PontonXPAdapterConfiguration config;
    private final Jaxb2Marshaller jaxb2Marshaller;
    private final OutboundMessageFactoryCollection outboundMessageFactoryCollection;
    private final MessengerConnection messengerConnection;

    public PontonXPAdapter(
            PontonXPAdapterConfiguration config,
            Jaxb2Marshaller jaxb2Marshaller,
            OutboundMessageFactoryCollection outboundMessageFactoryCollection
    ) throws IOException, ConnectionException {
        this.config = config;
        this.jaxb2Marshaller = jaxb2Marshaller;
        this.outboundMessageFactoryCollection = outboundMessageFactoryCollection;
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

    private InboundMessageStatusUpdate inboundMessageHandler(InboundMessage inboundMessage) {
        var messageType = inboundMessage.getInboundMetaData().getMessageType().getName().getValue();
        var messageVersion = inboundMessage.getInboundMetaData().getMessageType().getVersion().getValue();
        LOGGER.info("Received message type: '{}' (version '{}') with ConversationId: '{}'",
                    messageType,
                    messageVersion,
                    inboundMessage.getInboundMetaData().getConversationId().getValue());

        try {
            return switch (messageType) {
                case MessageCodes.Notification.ANSWER -> handleCMNotificationMessage(inboundMessage);
                case MessageCodes.Notification.ACCEPT -> handleCMAcceptNotificationMessage(inboundMessage);
                case MessageCodes.Notification.REJECT -> handleCMRejectNotificationMessage(inboundMessage);
                case MessageCodes.MASTER_DATA -> handleMasterDataMessage(inboundMessage);
                case MessageCodes.CONSUMPTION_RECORD -> handleConsumptionRecordMessage(inboundMessage);
                case MessageCodes.Revoke.CUSTOMER, MessageCodes.Revoke.IMPLICIT -> handleRevokeMessage(inboundMessage);
                default -> {
                    LOGGER.warn("Received message type '{}' (version '{}') is not supported.",
                                messageType,
                                messageVersion);
                    yield InboundMessageStatusUpdate.newBuilder()
                                                    .setInboundMessage(inboundMessage)
                                                    .setStatus(InboundStatusEnum.REJECTED)
                                                    .setStatusText("message type " + messageType + " version " + messageVersion + " is not supported.")
                                                    .build();
                }
            };
        } catch (XmlMappingException e) {
            LOGGER.error("Error while trying to unmarshal contents of message type '{}' in schema-version '{}'",
                         messageType,
                         messageVersion,
                         e);
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

    private void outboundMessageStatusUpdateHandler(OutboundMessageStatusUpdate outboundMessageStatusUpdate) {
        var conversationId = outboundMessageStatusUpdate.getStatusMetaData().getConversationId().getValue();

        LOGGER.info("Received status update for ConversationId: '{}' with result: '{}'",
                    conversationId,
                    outboundMessageStatusUpdate.getResult());
        switch (outboundMessageStatusUpdate.getResult()) {
            case SUCCESS ->  // success just indicates that the message was received by the other party (dso)
                    requestStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.RECEIVED,
                                                                      outboundMessageStatusUpdate.getDetailText(),
                                                                      conversationId));
            case CONFIG_ERROR, CONTENT_ERROR, TRANSMISSION_ERROR, FAILED ->
                    requestStatusSink.tryEmitNext(new CMRequestStatus(CMRequestStatus.Status.ERROR,
                                                                      outboundMessageStatusUpdate.getDetailText(),
                                                                      conversationId));
            default -> {
                // Ignore the other status updates
            }
        }
    }

    private InboundMessageStatusUpdate handleCMNotificationMessage(InboundMessage inboundMessage) throws IOException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var notification = (CMNotification) jaxb2Marshaller.unmarshal(new StreamSource(inputStream));
            var cmRequestId = notification.getProcessDirectory().getCMRequestId();
            ResponseDataType responseData = notification.getProcessDirectory().getResponseData().getFirst();
            var meteringPoint = responseData.getMeteringPoint();
            var responseCodes = responseData.getResponseCode();

            var status = new CMRequestStatus(CMRequestStatus.Status.DELIVERED,
                                             responseCodesToMessage(responseCodes, "CCMO request has been delivered."),
                                             notification.getProcessDirectory().getConversationId());
            status.setCmRequestId(cmRequestId);
            status.setMeteringPoint(meteringPoint);

            requestStatusSink.tryEmitNext(status);
            LOGGER.info("Received CMNotification '{}' for CMRequestId '{}' with ConversationId '{}'",
                        status.getStatus(),
                        cmRequestId,
                        status.getConversationId());

            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(InboundStatusEnum.SUCCESS)
                                             .setStatusText(CM_NOTIFICATION_PROCESSED)
                                             .build();
        }
    }

    private InboundMessageStatusUpdate handleCMAcceptNotificationMessage(InboundMessage inboundMessage) throws IOException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var notification = (CMNotification) jaxb2Marshaller.unmarshal(new StreamSource(inputStream));
            var cmRequestId = notification.getProcessDirectory().getCMRequestId();
            ResponseDataType responseData = notification.getProcessDirectory().getResponseData().getFirst();
            var consentId = responseData.getConsentId();
            var meteringPoint = responseData.getMeteringPoint();
            var responseCodes = responseData.getResponseCode();

            var status = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED,
                                             responseCodesToMessage(responseCodes, "CCMO request has been accepted."),
                                             notification.getProcessDirectory().getConversationId());
            status.setCmConsentId(consentId);
            status.setCmRequestId(cmRequestId);
            status.setMeteringPoint(meteringPoint);

            requestStatusSink.tryEmitNext(status);
            LOGGER.info(
                    "Received CMNotification: ACCEPTED for CMRequestId '{}' with ConversationId '{}' and ConsentId: '{}'",
                    cmRequestId,
                    status.getConversationId(),
                    consentId);

            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(InboundStatusEnum.SUCCESS)
                                             .setStatusText(CM_NOTIFICATION_PROCESSED)
                                             .build();
        }
    }

    private InboundMessageStatusUpdate handleCMRejectNotificationMessage(InboundMessage inboundMessage) throws IOException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var notification = (CMNotification) jaxb2Marshaller.unmarshal(new StreamSource(inputStream));
            var cmRequestId = notification.getProcessDirectory().getCMRequestId();
            var meteringPoint = notification.getProcessDirectory().getResponseData().getFirst().getMeteringPoint();

            var responseCodes = notification.getProcessDirectory().getResponseData().getFirst().getResponseCode();
            var reason = responseCodesToMessage(responseCodes, "DSO provided no reason for rejection.");

            var status = new CMRequestStatus(CMRequestStatus.Status.REJECTED,
                                             reason,
                                             notification.getProcessDirectory().getConversationId());
            status.setCmRequestId(cmRequestId);
            status.setMeteringPoint(meteringPoint);

            requestStatusSink.tryEmitNext(status);
            LOGGER.info("Received CMNotification: REJECTED for CMRequestId '{}' with  ConversationId '{}', reason '{}'",
                        cmRequestId,
                        status.getConversationId(),
                        reason);

            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(InboundStatusEnum.SUCCESS)
                                             .setStatusText(CM_NOTIFICATION_PROCESSED)
                                             .build();
        }
    }

    private InboundMessageStatusUpdate handleMasterDataMessage(InboundMessage inboundMessage) throws IOException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var masterData = (MasterData) jaxb2Marshaller.unmarshal(new StreamSource(inputStream));
            masterDataSink.tryEmitNext(masterData);

            LOGGER.info("Received master data with ConversationId '{}'",
                        masterData.getProcessDirectory().getConversationId());

            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(InboundStatusEnum.SUCCESS)
                                             .setStatusText("Master data successfully delivered to backend.")
                                             .build();
        }
    }

    private InboundMessageStatusUpdate handleConsumptionRecordMessage(InboundMessage inboundMessage) throws IOException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var consumptionRecord = (ConsumptionRecord) jaxb2Marshaller.unmarshal(new StreamSource(inputStream));
            // the process is documented here https://www.ebutilities.at/prozesse/230
            // we might have to create a ABLEHNUNG_CRMSG (CPNotification) if the message was not valid, see https://www.ebutilities.at/prozesse/230/marktnachrichten/615
            consumptionRecordSink.tryEmitNext(consumptionRecord);
            LOGGER.info("Received consumption record with ConversationId '{}'",
                        consumptionRecord.getProcessDirectory().getConversationId());

            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(InboundStatusEnum.SUCCESS)
                                             .setStatusText("ConsumptionRecord successfully delivered to backend.")
                                             .build();
        }
    }

    private InboundMessageStatusUpdate handleRevokeMessage(InboundMessage inboundMessage) throws IOException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            // convert stream to consumption record
            var cmRevoke = (CMRevoke) jaxb2Marshaller.unmarshal(new StreamSource(inputStream));

            cmRevokeSink.tryEmitNext(cmRevoke);
            LOGGER.info("Received revoke message for ConsentId '{}' with ConversationId '{}'",
                        cmRevoke.getProcessDirectory().getMeteringPoint(),
                        cmRevoke.getProcessDirectory().getConversationId());

            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(InboundStatusEnum.SUCCESS)
                                             .setStatusText("CMRevoke successfully delivered to backend.")
                                             .build();
        }
    }

    @NotNull
    private static String responseCodesToMessage(List<Integer> responseCodes, String defaultMessage) {
        return responseCodes.stream()
                            .map(ResponseCode::new)
                            .map(ResponseCode::toString)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse(defaultMessage);
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

    @Override
    public void sendCMRequest(CCMORequest request) throws TransmissionException {
        // convert request to XML
        var outboundMessage = outboundMessageFactoryCollection.activeCmRequestFactory().createOutboundMessage(request);
        try {
            LOGGER.atInfo()
                  .addArgument(() -> outboundMessage.getOutboundMetaData().getReceiverId().getValue())
                  .addArgument(request::cmRequestId)
                  .log("Sending CCMO request to DSO '{}' with RequestID '{}'");
            messengerConnection.sendMessage(outboundMessage);
        } catch (de.ponton.xp.adapter.api.TransmissionException e) {
            throw new TransmissionException(e);
        }

        requestStatusSink.tryEmitNext(
                new CMRequestStatus(
                        CMRequestStatus.Status.SENT,
                        "CCMO request has been sent",
                        request.messageId()
                )
        );
    }

    @Override
    public void sendCMRevoke(CCMORevoke revoke) throws TransmissionException {
        // convert revoke to XML
        var outboundMessage = outboundMessageFactoryCollection.activeCmRevokeFactory().createOutboundMessage(revoke);
        try {
            LOGGER.atInfo()
                  .addArgument(() -> outboundMessage.getOutboundMetaData().getReceiverId().getValue())
                  .addArgument(() -> revoke.permissionRequest().consentId())
                  .log("Sending CMRevoke to DSO '{}' with ConsentId '{}'");
            messengerConnection.sendMessage(outboundMessage);
        } catch (de.ponton.xp.adapter.api.TransmissionException e) {
            throw new TransmissionException(e);
        }
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

    @Override
    public Map<String, HealthState> health() {
        Map<String, HealthState> healthChecks = new HashMap<>();
        try {
            InetAddress address = InetAddress.getByName(config.hostname());
            healthChecks.put(PONTON_HOST, address.isReachable(PING_TIMEOUT) ? HealthState.UP : HealthState.DOWN);
        } catch (IOException e) {
            LOGGER.warn("Ponton XP Messenger Host not reachable", e);
            healthChecks.put(PONTON_HOST, HealthState.DOWN);
        }
        return healthChecks;
    }
}
