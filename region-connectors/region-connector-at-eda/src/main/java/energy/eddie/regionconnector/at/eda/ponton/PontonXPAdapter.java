package energy.eddie.regionconnector.at.eda.ponton;

import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.domainvalues.InboundStatusEnum;
import de.ponton.xp.adapter.api.messages.OutboundMessageStatusUpdate;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.dto.*;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ConsentData;
import energy.eddie.regionconnector.at.eda.ponton.messenger.CPNotificationMessageType;
import energy.eddie.regionconnector.at.eda.ponton.messenger.InboundMessageResult;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import energy.eddie.regionconnector.at.eda.ponton.messenger.PontonMessengerConnection;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;
import energy.eddie.regionconnector.at.eda.requests.CPRequestResult;
import energy.eddie.regionconnector.at.eda.services.IdentifiableConsumptionRecordService;
import energy.eddie.regionconnector.at.eda.services.IdentifiableMasterDataService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static de.ponton.xp.adapter.api.domainvalues.OutboundStatusEnum.*;


public class PontonXPAdapter implements EdaAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PontonXPAdapter.class);
    private final Sinks.Many<CMRequestStatus> requestStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<IdentifiableConsumptionRecord> consumptionRecordSink = Sinks.many()
                                                                                         .multicast()
                                                                                         .onBackpressureBuffer();
    private final Sinks.Many<EdaCMRevoke> cmRevokeSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<IdentifiableMasterData> masterDataSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<CPRequestResult> cpRequestResultSink = Sinks.many().multicast().onBackpressureBuffer();
    private final PontonMessengerConnection pontonMessengerConnection;
    private final IdentifiableConsumptionRecordService identifiableConsumptionRecordService;
    private final IdentifiableMasterDataService identifiableMasterDataService;
    private final TaskScheduler scheduler;
    /**
     * Set of conversationIds for CPRequests that have been sent to Ponton. Used to distinguish between CPRequests and CMRequests in the OutboundMessageStatusUpdateHandler.
     */
    private final Set<String> cpRequestSet = ConcurrentHashMap.newKeySet();

    public PontonXPAdapter(
            PontonMessengerConnection pontonMessengerConnection,
            IdentifiableConsumptionRecordService identifiableConsumptionRecordService,
            IdentifiableMasterDataService identifiableMasterDataService,
            TaskScheduler scheduler
    ) {
        this.pontonMessengerConnection = pontonMessengerConnection
                .withOutboundMessageStatusUpdateHandler(this::outboundMessageStatusUpdateHandler)
                .withCMNotificationHandler(this::handleCMNotificationMessage)
                .withMasterDataHandler(this::handleMasterDataMessage)
                .withConsumptionRecordHandler(this::handleConsumptionRecordMessage)
                .withCMRevokeHandler(this::handleRevokeMessage)
                .withCPNotificationHandler(this::handleCPNotificationMessage);
        this.identifiableConsumptionRecordService = identifiableConsumptionRecordService;
        this.identifiableMasterDataService = identifiableMasterDataService;
        this.scheduler = scheduler;
    }

    @Override
    public void close() {
        requestStatusSink.tryEmitComplete();
        consumptionRecordSink.tryEmitComplete();
        masterDataSink.tryEmitComplete();
        cmRevokeSink.tryEmitComplete();
        cpRequestResultSink.tryEmitComplete();
        pontonMessengerConnection.close();
    }

    @Override
    public Flux<CMRequestStatus> getCMRequestStatusStream() {
        return requestStatusSink.asFlux();
    }

    @Override
    public Flux<IdentifiableConsumptionRecord> getConsumptionRecordStream() {
        return consumptionRecordSink.asFlux();
    }

    @Override
    public Flux<EdaCMRevoke> getCMRevokeStream() {
        return cmRevokeSink.asFlux();
    }

    @Override
    public Flux<IdentifiableMasterData> getMasterDataStream() {
        return masterDataSink.asFlux();
    }

    @Override
    public Flux<CPRequestResult> getCPRequestResultStream() {
        return cpRequestResultSink.asFlux();
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
    public void sendCPRequest(CPRequestCR cpRequestCR) throws TransmissionException {
        try {
            LOGGER.atInfo()
                  .addArgument(cpRequestCR::dsoId)
                  .addArgument(cpRequestCR::messageId)
                  .log("Sending CPRequest to DSO '{}' with ConversationId '{}'");
            cpRequestSet.add(cpRequestCR.messageId());
            pontonMessengerConnection.sendCPRequest(cpRequestCR);
        } catch (de.ponton.xp.adapter.api.TransmissionException | ConnectionException e) {
            cpRequestSet.remove(cpRequestCR.messageId());
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

    private void outboundMessageStatusUpdateHandler(OutboundMessageStatusUpdate outboundMessageStatusUpdate) {
        var conversationId = outboundMessageStatusUpdate.getStatusMetaData().getConversationId().getValue();

        LOGGER.atInfo()
              .addArgument(conversationId)
              .addArgument(outboundMessageStatusUpdate::getResult)
              .log("Received status update for ConversationId: '{}' with result: '{}'");
        var cpRequest = cpRequestSet.remove(conversationId);
        var result = outboundMessageStatusUpdate.getResult();
        if (!List.of(CONFIG_ERROR, CONTENT_ERROR, TRANSMISSION_ERROR, FAILED).contains(result)) {
            LOGGER.info("Ignoring status update for ConversationId: '{}' with result: '{}'", conversationId, result);
            return;
        }
        if (cpRequest) {
            // If the outgoing message was a CPRequest, complete the future with an error
            LOGGER.atDebug()
                  .addArgument(conversationId)
                  .addArgument(outboundMessageStatusUpdate::getResult)
                  .addArgument(outboundMessageStatusUpdate::getDetailText)
                  .log("Ponton could not send CPRequest for ConversationId '{}': '{}'. Detail: '{}'");
            cpRequestResultSink.emitNext(
                    new CPRequestResult(conversationId, CPRequestResult.Result.PONTON_ERROR),
                    Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1))
            );
        } else {
            var status = new CMRequestStatus(
                    NotificationMessageType.PONTON_ERROR,
                    conversationId,
                    outboundMessageStatusUpdate.getDetailText()
            );
            requestStatusSink.emitNext(status, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
        }
    }

    private InboundMessageResult handleCMNotificationMessage(
            EdaCMNotification notification,
            NotificationMessageType notificationMessageType
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

        var status = new CMRequestStatus(
                notificationMessageType,
                conversationId,
                cmRequestId,
                notification.responseData().stream().map(ConsentData::fromResponseData).toList()
        );
        status.consentData().forEach(consentData -> LOGGER
                .atInfo()
                .addArgument(status::messageType)
                .addArgument(status::cmRequestId)
                .addArgument(() -> consentData.cmConsentId()
                                              .map(consentId -> " (ConsentId '" + consentId + "')")
                                              .orElse(Strings.EMPTY))
                .addArgument(status::conversationId)
                .addArgument(() -> Arrays.toString(consentData.responseCodes().toArray()))
                .log("Received CMNotification: {} for CMRequestId '{}'{} with ConversationId '{}', response code: '{}'")
        );

        var result = requestStatusSink.tryEmitNext(status);
        return handleEmitResult(result);
    }

    private InboundMessageResult handleCPNotificationMessage(
            EdaCPNotification cpNotification,
            CPNotificationMessageType cpNotificationMessageType
    ) {
        LOGGER.atInfo()
              .addArgument(cpNotification::conversationId)
              .addArgument(cpNotificationMessageType)
              .log("Received CPNotification for ConversationId '{}' with NotificationType '{}'");

        CPRequestResult.Result result = CPRequestResult.Result.UNKNOWN_RESPONSE_CODE_ERROR;
        for (Integer responseCode : cpNotification.responseCodes()) {
            result = CPRequestResult.Result.fromResponseCode(responseCode);
            if (result == CPRequestResult.Result.ACCEPTED) {
                break;
            }
        }

        var emitResult = cpRequestResultSink.tryEmitNext(
                new CPRequestResult(cpNotification.originalMessageId(), result)
        );
        return handleEmitResult(emitResult);
    }

    private InboundMessageResult handleMasterDataMessage(EdaMasterData masterData) {
        var identifiableMasterData = identifiableMasterDataService.mapToIdentifiableMasterData(masterData);
        if (identifiableMasterData.isEmpty()) {
            /*
             * This rescheduling is essentially only necessary for the case, where a customer accepts a PermissionRequest for multiple MeteringPoints
             * and when the DSO then sends the Data for the MeteringPoints before the "ZUSTIMMUNG_CCMO".
             * The reason for this, is that until we process the "ZUSTIMMUNG_CCMO", identifiableMasterDataService.mapToIdentifiableMasterData
             * will always return empty, as it will not be able to find a PermissionRequest for the received data.
             * Once we receive the "ZUSTIMMUNG_CCMO" the CCMOAcceptHandler will create a PermissionRequest for every MeteringPoints contained.
             */
            var date = masterData.documentCreationDateTime();
            scheduleMessageResend(date, masterData.messageId());
            LOGGER.atWarn()
                  .addArgument(masterData::conversationId)
                  .log("Received master data for conversationId '{}' before ZUSTIMMUNG_CCMO");
            return new InboundMessageResult(
                    InboundStatusEnum.REJECTED,
                    "Received master data before ZUSTIMMUNG_CCMO, trying again later."
            );
        }
        var idm = identifiableMasterData.get();
        var pr = idm.permissionRequest();
        var status = pr.status();
        if (status == PermissionProcessStatus.FULFILLED || status == PermissionProcessStatus.REJECTED || status == PermissionProcessStatus.EXTERNALLY_TERMINATED) {
            var permissionId = pr.permissionId();
            LOGGER.info("Got master data for {} permission request {}, will be ignored",
                        status,
                        permissionId);
            return new InboundMessageResult(
                    InboundStatusEnum.SUCCESS,
                    "Data was already received for this permission request %s".formatted(permissionId)
            );
        }

        var result = masterDataSink.tryEmitNext(idm);
        return handleEmitResult(result);
    }

    private InboundMessageResult handleConsumptionRecordMessage(EdaConsumptionRecord consumptionRecord) {
        var identifiableConsumptionRecord = identifiableConsumptionRecordService
                .mapToIdentifiableConsumptionRecord(consumptionRecord);

        if (identifiableConsumptionRecord.isEmpty()) {
            /*
             * This rescheduling is essentially only necessary for the case, where a customer accepts a PermissionRequest for multiple MeteringPoints
             * and when the DSO then sends the Data for the MeteringPoints before the "ZUSTIMMUNG_CCMO".
             * The reason for this, is that until we process the "ZUSTIMMUNG_CCMO", identifiableConsumptionRecordService.mapToIdentifiableConsumptionRecord
             * will always return empty, as it will not be able to find a PermissionRequest for the received data.
             * Once we receive the "ZUSTIMMUNG_CCMO" the CCMOAcceptHandler will create a PermissionRequest for every MeteringPoints contained.
             */
            var date = consumptionRecord.documentCreationDateTime();
            scheduleMessageResend(date, consumptionRecord.messageId());
            return new InboundMessageResult(
                    InboundStatusEnum.REJECTED,
                    "No permission requests found for consumption record, trying again later."
            );
        }

        // the process is documented here https://www.ebutilities.at/prozesse/230#
        var result = consumptionRecordSink.tryEmitNext(identifiableConsumptionRecord.get());
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

    @SuppressWarnings("FutureReturnValueIgnored") // we do not care about the result of the scheduled task
    private void scheduleMessageResend(ZonedDateTime date, String messageId) {
        LOGGER.atInfo()
              .addArgument(messageId)
              .addArgument(date::toString)
              .log("Scheduling resend of failed message '{}' that arrived after '{}'");
        scheduler.schedule(
                () -> pontonMessengerConnection.resendFailedMessage(date, messageId),
                Instant.now().plusSeconds(30)
        );
    }
}
