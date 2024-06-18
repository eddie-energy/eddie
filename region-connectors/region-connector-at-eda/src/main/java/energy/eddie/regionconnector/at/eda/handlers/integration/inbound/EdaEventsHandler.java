package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEventFactory;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EdaEventsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaEventsHandler.class);
    private final Outbox outbox;
    private final AtPermissionRequestRepository repository;
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    private final DataNeedsService dataNeedsService;
    private final ValidatedEventFactory validatedEventFactory;

    public EdaEventsHandler(
            EdaAdapter edaAdapter,
            Outbox outbox,
            AtPermissionRequestRepository repository,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService,
            ValidatedEventFactory validatedEventFactory
    ) {
        this.outbox = outbox;
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
        this.validatedEventFactory = validatedEventFactory;
        edaAdapter.getCMRequestStatusStream()
                  .subscribe(this::processIncomingCmStatusMessages);
        this.dataNeedCalculationService = dataNeedCalculationService;
    }

    /**
     * Process a CMRequestStatus and emit a ConnectionStatusMessage if possible, also adds connectionId and permissionId
     * for identification
     *
     * @param cmRequestStatus the CMRequestStatus to process
     */
    private void processIncomingCmStatusMessages(CMRequestStatus cmRequestStatus) {
        var optionalPermissionRequest = repository.findByConversationIdOrCMRequestId(
                cmRequestStatus.conversationId(),
                cmRequestStatus.cmRequestId()
        );
        if (optionalPermissionRequest.isEmpty()) {
            // should not happen if a persistent mapping is used.
            LOGGER.error("Received CMRequestStatus for unknown conversationId '{}' or requestId '{}' with payload: '{}'",
                         cmRequestStatus.conversationId(), cmRequestStatus.cmRequestId(), cmRequestStatus);
            return;
        }
        transitionPermissionRequest(cmRequestStatus, optionalPermissionRequest.get());
    }

    private void transitionPermissionRequest(CMRequestStatus cmRequestStatus, AtPermissionRequest request) {
        String permissionId = request.permissionId();
        switch (cmRequestStatus.messageType()) {
            case CCMO_ACCEPT -> handleCCMOAccept(cmRequestStatus, permissionId);
            case PONTON_ERROR -> handlePontonError(cmRequestStatus, request, permissionId);
            case CCMO_REJECT -> handleCCMOReject(cmRequestStatus, request, permissionId);
            case CCMO_ANSWER -> outbox.commit(
                    new EdaAnswerEvent(permissionId,
                                       PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                       cmRequestStatus.message())
            );
            case CCMS_ANSWER -> outbox.commit(
                    new SimpleEvent(permissionId, PermissionProcessStatus.EXTERNALLY_TERMINATED)
            );
            case CCMS_REJECT -> handleCCMSReject(cmRequestStatus, permissionId);
        }
    }

    private void handleCCMOAccept(CMRequestStatus cmRequestStatus, String permissionId) {
        Optional<String> meteringPoint = cmRequestStatus.getMeteringPoint();
        if (meteringPoint.isEmpty()) {
            LOGGER.error(
                    "Metering point id is missing in ACCEPTED CMRequestStatus message for permission id: '{}'",
                    permissionId);
            return;
        }

        Optional<String> cmConsentId = cmRequestStatus.getCMConsentId();
        if (cmConsentId.isEmpty()) {
            LOGGER.error("Got accept message without consent id for permission request with permission id '{}'",
                         permissionId);
            return;
        }
        outbox.commit(new AcceptedEvent(
                permissionId,
                meteringPoint.get(),
                cmConsentId.get(),
                cmRequestStatus.message()
        ));
    }

    private void handlePontonError(CMRequestStatus cmRequestStatus, AtPermissionRequest request, String permissionId) {
        // If the DSO does not exist EDA will respond with an error without sending a received-message.
        // In that case the error message is an implicit received-message.
        if (request.status() == PermissionProcessStatus.VALIDATED) {
            outbox.commit(
                    new EdaAnswerEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                       cmRequestStatus.message()));
        }
        outbox.commit(
                new EdaAnswerEvent(permissionId, PermissionProcessStatus.INVALID, cmRequestStatus.message())
        );
    }

    private void handleCCMOReject(
            CMRequestStatus cmRequestStatus,
            AtPermissionRequest permissionRequest,
            String permissionId
    ) {
        var message = cmRequestStatus.message();
        for (Integer statusCode : cmRequestStatus.statusCodes()) {
            if (statusCode == ResponseCode.CmReqOnl.REJECTED) {
                outbox.commit(new EdaAnswerEvent(permissionId, PermissionProcessStatus.REJECTED, message));
                return;
            }

            if (statusCode == ResponseCode.CmReqOnl.TIMEOUT) {
                outbox.commit(new EdaAnswerEvent(permissionId, PermissionProcessStatus.TIMED_OUT, message));
                return;
            }
            if (statusCode == ResponseCode.CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE) {
                var wasRetried = retryWithHigherGranularity(permissionRequest);
                if (wasRetried) return;
            }
        }

        outbox.commit(new EdaAnswerEvent(permissionId, PermissionProcessStatus.INVALID, message));
    }

    /**
     * Handle a CCMS_REJECT message. We mark the permission request as externally terminated no matter the reason. This
     * means even if we still receive data from the DSO we will not process it.
     * <p>
     * We do this, since the response codes {@link ResponseCode.CmRevSP#NO_CONSENT_PRESENT} and
     * {@link ResponseCode.CmRevSP#CONSENT_ID_EXPIRED} both indicate that the consent is not valid anymore, so we should
     * not receive any data anymore. This case should only occur if the eligible party tries to terminate a permission
     * request that has already been {@link PermissionProcessStatus#FULFILLED}.
     * <p>
     * The response code {@link ResponseCode.CmRevSP#INVALID_PROCESSDATE} should never occur since we set the process
     * date of the {@link CCMORevoke} to the current date.
     * <p>
     * The last option {@link ResponseCode.CmRevSP#CONSENT_AND_METERINGPOINT_DO_NOT_MATCH} should never occur, if this
     * occurs we have a bug in our system.
     */
    private void handleCCMSReject(CMRequestStatus cmRequestStatus, String permissionId) {
        for (Integer statusCode : cmRequestStatus.statusCodes()) {
            if (statusCode == ResponseCode.CmRevSP.INVALID_PROCESSDATE) {
                LOGGER.atError()
                      .addArgument(permissionId)
                      .log("Received a message that indicates that we send an invalid process date for the CMRevoke message for permission request '{}', this should never happen");
            } else if (statusCode == ResponseCode.CmRevSP.CONSENT_AND_METERINGPOINT_DO_NOT_MATCH) {
                LOGGER.atError()
                      .addArgument(permissionId)
                      .log("Received a message that indicates that the consent id and metering point id provided in the CMRevoke message do not match for permission request '{}', this should never happen");
            }
        }

        outbox.commit(new EdaAnswerEvent(permissionId,
                                         PermissionProcessStatus.EXTERNALLY_TERMINATED,
                                         cmRequestStatus.message()));
    }

    private boolean retryWithHigherGranularity(AtPermissionRequest permissionRequest) {
        if (permissionRequest.granularity() != AllowedGranularity.PT15M) {
            return false;
        }
        var dataNeed = dataNeedsService.getById(permissionRequest.dataNeedId());
        var calc = dataNeedCalculationService.calculate(dataNeed);
        if (calc.granularities() == null || !calc.granularities().contains(Granularity.P1D)) {
            return false;
        }

        outbox.commit(validatedEventFactory.createValidatedEvent(
                permissionRequest.permissionId(),
                permissionRequest.start(),
                permissionRequest.end(),
                AllowedGranularity.P1D
        ));
        return true;
    }
}
