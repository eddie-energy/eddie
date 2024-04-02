package energy.eddie.regionconnector.at.eda.ponton;

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
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.dto.ResponseData;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.ponton.messages.InboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.XmlMappingException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PontonXPAdapter implements EdaAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PontonXPAdapter.class);
    private static final String PONTON_HOST = "pontonHost";
    private static final int PING_TIMEOUT = 2000;
    private final Sinks.Many<CMRequestStatus> requestStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<EdaConsumptionRecord> consumptionRecordSink = Sinks.many()
                                                                                .unicast()
                                                                                .onBackpressureBuffer();
    private final Sinks.Many<EdaCMRevoke> cmRevokeSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<EdaMasterData> masterDataSink = Sinks.many().multicast().onBackpressureBuffer();
    private final PontonXPAdapterConfiguration config;
    private final OutboundMessageFactoryCollection outboundMessageFactoryCollection;
    private final InboundMessageFactoryCollection inboundMessageFactoryCollection;
    private final MessengerConnection messengerConnection;

    public PontonXPAdapter(
            PontonXPAdapterConfiguration config,
            OutboundMessageFactoryCollection outboundMessageFactoryCollection,
            InboundMessageFactoryCollection inboundMessageFactoryCollection
    ) throws IOException, ConnectionException {
        this.config = config;
        this.outboundMessageFactoryCollection = outboundMessageFactoryCollection;
        this.inboundMessageFactoryCollection = inboundMessageFactoryCollection;
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
                case MessageCodes.Notification.ANSWER ->
                        handleCMNotificationMessage(inboundMessage, CMRequestStatus.Status.DELIVERED);
                case MessageCodes.Notification.ACCEPT ->
                        handleCMNotificationMessage(inboundMessage, CMRequestStatus.Status.ACCEPTED);
                case MessageCodes.Notification.REJECT ->
                        handleCMNotificationMessage(inboundMessage, CMRequestStatus.Status.REJECTED);
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

    private InboundMessageStatusUpdate handleCMNotificationMessage(
            InboundMessage inboundMessage,
            CMRequestStatus.Status requestStatus
    ) throws IOException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var notification = inboundMessageFactoryCollection.activeCMNotificationFactory()
                                                              .parseInputStream(inputStream);

            var cmRequestId = notification.cmRequestId();
            var conversationId = notification.conversationId();
            for (ResponseData responseData : notification.responseData()) {
                var status = new CMRequestStatus(
                        requestStatus,
                        responseCodesToMessage(responseData.responseCodes()),
                        conversationId
                );
                status.setCmRequestId(cmRequestId);
                status.setMeteringPoint(responseData.meteringPoint());
                status.setCmConsentId(responseData.consentId());

                requestStatusSink.emitNext(status, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(5)));
                LOGGER.atInfo()
                      .addArgument(status::getStatus)
                      .addArgument(status::getCMRequestId)
                      .addArgument(() -> status.getCMConsentId()
                                               .map(consentId -> " (ConsentId '" + consentId + "')")
                                               .orElse(Strings.EMPTY))
                      .addArgument(status::getConversationId)
                      .addArgument(status::getMessage)
                      .log("Received CMNotification: {} for CMRequestId '{}'{} with ConversationId '{}', reason '{}'");
            }
        }

        return InboundMessageStatusUpdate.newBuilder()
                                         .setInboundMessage(inboundMessage)
                                         .setStatus(InboundStatusEnum.SUCCESS)
                                         .setStatusText("CMNotification processed")
                                         .build();
    }

    private InboundMessageStatusUpdate handleMasterDataMessage(InboundMessage inboundMessage) throws IOException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var masterData = inboundMessageFactoryCollection.activeMasterDataFactory()
                                                            .parseInputStream(inputStream);
            masterDataSink.tryEmitNext(masterData);
            LOGGER.atInfo()
                  .addArgument(masterData::conversationId)
                  .log("Received master data with ConversationId '{}'");

            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(InboundStatusEnum.SUCCESS)
                                             .setStatusText("Master data successfully delivered to backend.")
                                             .build();
        }
    }

    private InboundMessageStatusUpdate handleConsumptionRecordMessage(InboundMessage inboundMessage) throws IOException {
        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var consumptionRecord = inboundMessageFactoryCollection.activeConsumptionRecordFactory()
                                                                   .parseInputStream(inputStream);
            // the process is documented here https://www.ebutilities.at/prozesse/230
            consumptionRecordSink.tryEmitNext(consumptionRecord);
            LOGGER.atInfo()
                  .addArgument(consumptionRecord::conversationId)
                  .log("ConsumptionRecord successfully delivered to backend with ConversationId '{}'");

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
            var cmRevoke = inboundMessageFactoryCollection.activeCMRevokeFactory()
                                                          .parseInputStream(inputStream);

            cmRevokeSink.emitNext(cmRevoke, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(5)));
            LOGGER.atInfo()
                  .addArgument(cmRevoke::consentId)
                  .addArgument(cmRevoke::consentEnd)
                  .log("Received CMRevoke for ConsentId '{}' with ConsentEnd '{}'");

            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(InboundStatusEnum.SUCCESS)
                                             .setStatusText("CMRevoke successfully delivered to backend.")
                                             .build();
        }
    }

    @NotNull
    private static String responseCodesToMessage(List<Integer> responseCodes) {
        return responseCodes.stream()
                            .map(ResponseCode::new)
                            .map(ResponseCode::toString)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("No response codes provided.");
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
    public Flux<EdaConsumptionRecord> getConsumptionRecordStream() {
        return consumptionRecordSink.asFlux();
    }

    @Override
    public Flux<EdaCMRevoke> getCMRevokeStream() {
        return cmRevokeSink.asFlux();
    }

    @Override
    public Flux<EdaMasterData> getMasterDataStream() {
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
