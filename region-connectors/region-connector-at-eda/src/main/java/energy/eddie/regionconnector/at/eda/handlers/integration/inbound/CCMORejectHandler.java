package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEventFactory;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CCMORejectHandler {
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    private final ValidatedEventFactory validatedEventFactory;
    private final AtPermissionRequestRepository repository;

    private final Outbox outbox;

    public CCMORejectHandler(
            DataNeedCalculationService<DataNeed> dataNeedCalculationService,
            ValidatedEventFactory validatedEventFactory,
            AtPermissionRequestRepository repository,
            Outbox outbox
    ) {
        this.dataNeedCalculationService = dataNeedCalculationService;
        this.validatedEventFactory = validatedEventFactory;
        this.repository = repository;
        this.outbox = outbox;
    }

    public void handleCCMOReject(
            CMRequestStatus cmRequestStatus
    ) {
        var permissionRequests = repository.findByConversationIdOrCMRequestId(
                cmRequestStatus.conversationId(),
                cmRequestStatus.cmRequestId()
        );
        for (var projection : permissionRequests) {
            handlePermissionRequestReject(
                    cmRequestStatus,
                    EdaPermissionRequest.fromProjection(projection)
            );
        }
    }

    private void handlePermissionRequestReject(CMRequestStatus cmRequestStatus, AtPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        var message = cmRequestStatus.message();
        for (Integer statusCode : cmRequestStatus.consentData().getFirst().responseCodes()) {
            var handled = switch (statusCode) {
                case ResponseCode.CmReqOnl.REJECTED -> emitEvent(new EdaAnswerEvent(permissionId,
                                                                                    PermissionProcessStatus.REJECTED,
                                                                                    message));
                case ResponseCode.CmReqOnl.CONSENT_REQUEST_ID_ALREADY_EXISTS ->
                        emitEvent(validatedEventFactory.createValidatedEvent(
                                permissionRequest.permissionId(),
                                permissionRequest.start(),
                                permissionRequest.end(),
                                permissionRequest.granularity()
                        ));
                case ResponseCode.CmReqOnl.TIMEOUT -> emitEvent(new EdaAnswerEvent(permissionId,
                                                                                   PermissionProcessStatus.TIMED_OUT,
                                                                                   message));
                case ResponseCode.CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE ->
                        retryWithHigherGranularity(permissionRequest);
                default -> false;
            };
            if (handled) return;
        }

        outbox.commit(new EdaAnswerEvent(permissionId, PermissionProcessStatus.INVALID, message));
    }

    private boolean emitEvent(PermissionEvent event) {
        outbox.commit(event);
        return true;
    }

    private boolean retryWithHigherGranularity(AtPermissionRequest permissionRequest) {
        if (permissionRequest.granularity() != AllowedGranularity.PT15M) {
            return false;
        }
        var calc = dataNeedCalculationService.calculate(permissionRequest.dataNeedId());
        if (!(calc instanceof ValidatedHistoricalDataDataNeedResult(
                List<Granularity> granularities,
                // False positive
                @SuppressWarnings("java:S1481")Timeframe ignored1,
                @SuppressWarnings("java:S1481")Timeframe ignored2
        ))
            || !granularities.contains(Granularity.P1D)) {
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
