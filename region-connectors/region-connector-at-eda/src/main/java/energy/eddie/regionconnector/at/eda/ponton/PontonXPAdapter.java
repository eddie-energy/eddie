package energy.eddie.regionconnector.at.eda.ponton;

import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.domainvalues.InboundStatusEnum;
import de.ponton.xp.adapter.api.messages.OutboundMessageStatusUpdate;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.dto.EdaCMNotification;
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.ponton.messenger.InboundMessageResult;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationType;
import energy.eddie.regionconnector.at.eda.ponton.messenger.PontonMessengerConnection;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PontonXPAdapter implements EdaAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PontonXPAdapter.class);
    private static final String PONTON_HOST = "pontonHost";
    private final Sinks.Many<CMRequestStatus> requestStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<EdaConsumptionRecord> consumptionRecordSink = Sinks.many()
                                                                                .unicast()
                                                                                .onBackpressureBuffer();
    private final Sinks.Many<EdaCMRevoke> cmRevokeSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<EdaMasterData> masterDataSink = Sinks.many().multicast().onBackpressureBuffer();
    private final PontonMessengerConnection pontonMessengerConnection;

    public PontonXPAdapter(PontonMessengerConnection pontonMessengerConnection) {
        this.pontonMessengerConnection = pontonMessengerConnection
                .withOutboundMessageStatusUpdateHandler(this::outboundMessageStatusUpdateHandler)
                .withCMNotificationHandler(this::handleCMNotificationMessage)
                .withMasterDataHandler(this::handleMasterDataMessage)
                .withConsumptionRecordHandler(this::handleConsumptionRecordMessage)
                .withCMRevokeHandler(this::handleRevokeMessage);
    }

    private void outboundMessageStatusUpdateHandler(OutboundMessageStatusUpdate outboundMessageStatusUpdate) {
        var conversationId = outboundMessageStatusUpdate.getStatusMetaData().getConversationId().getValue();

        LOGGER.atInfo()
              .addArgument(conversationId)
              .addArgument(outboundMessageStatusUpdate::getResult)
              .log("Received status update for ConversationId: '{}' with result: '{}'");
        switch (outboundMessageStatusUpdate.getResult()) {
            case SUCCESS ->  // success just indicates that the message was received by the other party (dso)
            {
                CMRequestStatus status = new CMRequestStatus(
                        CMRequestStatus.Status.RECEIVED,
                        outboundMessageStatusUpdate.getDetailText(),
                        conversationId
                );
                requestStatusSink.emitNext(status, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
            }
            case CONFIG_ERROR, CONTENT_ERROR, TRANSMISSION_ERROR, FAILED -> {
                CMRequestStatus status = new CMRequestStatus(
                        CMRequestStatus.Status.ERROR,
                        outboundMessageStatusUpdate.getDetailText(),
                        conversationId
                );
                requestStatusSink.emitNext(status, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
            }
            default -> {
                // Ignore the other status updates
            }
        }
    }

    private InboundMessageResult handleCMNotificationMessage(
            EdaCMNotification notification,
            NotificationType notificationType
    ) {
        var cmRequestId = notification.cmRequestId();
        var conversationId = notification.conversationId();
        if (notification.responseData().isEmpty()) {
            LOGGER.atWarn()
                  .addArgument(cmRequestId)
                  .addArgument(conversationId)
                  .log("Received empty CMNotification for CMRequestId '{}' with ConversationId '{}'");
            return new InboundMessageResult(
                    InboundStatusEnum.SUCCESS,
                    "EdaCMNotification with empty response data received."
            );
        }
        if (notification.responseData().size() > 1) {
            LOGGER.atWarn()
                  .addArgument(cmRequestId)
                  .addArgument(conversationId)
                  .log("Received CMNotification with multiple response data for CMRequestId '{}' with ConversationId '{}', processing only the first one.");
        }

        var responseData = notification.responseData().getFirst();
        var status = new CMRequestStatus(
                switch (notificationType) {
                    case ANSWER -> CMRequestStatus.Status.DELIVERED;
                    case ACCEPT -> CMRequestStatus.Status.ACCEPTED;
                    case REJECT -> CMRequestStatus.Status.REJECTED;
                },
                responseCodesToMessage(responseData.responseCodes()),
                conversationId
        );
        status.setCmRequestId(cmRequestId);
        status.setMeteringPoint(responseData.meteringPoint());
        status.setCmConsentId(responseData.consentId());
        LOGGER.atInfo()
              .addArgument(status::getStatus)
              .addArgument(status::getCMRequestId)
              .addArgument(() -> status.getCMConsentId()
                                       .map(consentId -> " (ConsentId '" + consentId + "')")
                                       .orElse(Strings.EMPTY))
              .addArgument(status::getConversationId)
              .addArgument(status::getMessage)
              .log("Received CMNotification: {} for CMRequestId '{}'{} with ConversationId '{}', reason '{}'");

        var result = requestStatusSink.tryEmitNext(status);
        return handleEmitResult(result);
    }

    private InboundMessageResult handleMasterDataMessage(EdaMasterData masterData) {
        LOGGER.atInfo()
              .addArgument(masterData::conversationId)
              .log("Received master data with ConversationId '{}'");
        var result = masterDataSink.tryEmitNext(masterData);
        return handleEmitResult(result);
    }

    private InboundMessageResult handleConsumptionRecordMessage(EdaConsumptionRecord consumptionRecord) {
        // the process is documented here https://www.ebutilities.at/prozesse/230#
        LOGGER.atInfo()
              .addArgument(consumptionRecord::conversationId)
              .log("ConsumptionRecord successfully delivered to backend with ConversationId '{}'");
        var result = consumptionRecordSink.tryEmitNext(consumptionRecord);
        return handleEmitResult(result);
    }

    private InboundMessageResult handleRevokeMessage(EdaCMRevoke cmRevoke) {
        LOGGER.atInfo()
              .addArgument(cmRevoke::consentId)
              .addArgument(cmRevoke::consentEnd)
              .log("Received CMRevoke for ConsentId '{}' with ConsentEnd '{}'");
        var result = cmRevokeSink.tryEmitNext(cmRevoke);
        return handleEmitResult(result);
    }

    @NotNull
    private static String responseCodesToMessage(List<Integer> responseCodes) {
        return responseCodes.stream()
                            .map(ResponseCode::new)
                            .map(ResponseCode::toString)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("No response codes provided.");
    }

    private InboundMessageResult handleEmitResult(Sinks.EmitResult emitResult) {
        return switch (emitResult) {
            case OK -> new InboundMessageResult(
                    InboundStatusEnum.SUCCESS, "Successfully emitted object to backend."
            );
            case FAIL_OVERFLOW, FAIL_ZERO_SUBSCRIBER, FAIL_NON_SERIALIZED, FAIL_CANCELLED -> new InboundMessageResult(
                    InboundStatusEnum.TEMPORARY_ERROR, "Error while emitting object to backend: " + emitResult
            );
            case FAIL_TERMINATED -> new InboundMessageResult(
                    InboundStatusEnum.REJECTED, "Sink terminated. Failed to deliver to backend."
            );
        };
    }

    @Override
    public void close() {
        requestStatusSink.tryEmitComplete();
        consumptionRecordSink.tryEmitComplete();
        masterDataSink.tryEmitComplete();
        cmRevokeSink.tryEmitComplete();
        pontonMessengerConnection.close();
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
        try {
            LOGGER.atInfo()
                  .addArgument(request::dsoId)
                  .addArgument(request::cmRequestId)
                  .log("Sending CCMO request to DSO '{}' with RequestID '{}'");
            pontonMessengerConnection.sendCMRequest(request);
        } catch (de.ponton.xp.adapter.api.TransmissionException | ConnectionException e) {
            throw new TransmissionException(e);
        }

        CMRequestStatus status = new CMRequestStatus(
                CMRequestStatus.Status.SENT,
                "CCMO request has been sent",
                request.messageId()
        );
        requestStatusSink.emitNext(
                status,
                Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1))
        );
    }

    @Override
    public void sendCMRevoke(CCMORevoke revoke) throws TransmissionException {
        try {
            LOGGER.atInfo()
                  .addArgument(() -> revoke.permissionRequest().dataSourceInformation().permissionAdministratorId())
                  .addArgument(() -> revoke.permissionRequest().consentId())
                  .log("Sending CMRevoke to DSO '{}' with ConsentId '{}'");
            pontonMessengerConnection.sendCMRevoke(revoke);
        } catch (de.ponton.xp.adapter.api.TransmissionException | ConnectionException e) {
            throw new TransmissionException(e);
        }
    }

    @Override
    public void start() throws TransmissionException {
        try {
            pontonMessengerConnection.start();
            LOGGER.info("Ponton XP adapter started.");
        } catch (de.ponton.xp.adapter.api.TransmissionException e) {
            throw new TransmissionException(e);
        }
    }

    @Override
    public Map<String, HealthState> health() {
        Map<String, HealthState> healthChecks = new HashMap<>();
        var status = pontonMessengerConnection.messengerStatus();
        healthChecks.put(PONTON_HOST, status.ok() ? HealthState.UP : HealthState.DOWN);
        status.healthChecks()
              .values()
              .forEach(healthCheck -> healthChecks.put(
                      PONTON_HOST + "." + healthCheck.name(),
                      healthCheck.ok() ? HealthState.UP : HealthState.DOWN
              ));
        return healthChecks;
    }
}
