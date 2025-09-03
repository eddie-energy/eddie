package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CCMSHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CCMSHandler.class);
    private final Outbox outbox;
    private final AtPermissionRequestRepository repository;

    public CCMSHandler(Outbox outbox, AtPermissionRequestRepository repository) {
        this.outbox = outbox;
        this.repository = repository;
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
    public void handleCCMSReject(CMRequestStatus cmRequestStatus) {
        for (var permissionRequest : repository.findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(),
                                                                                                  cmRequestStatus.cmRequestId())
                                               .stream().map(EdaPermissionRequest::fromProjection).toList()) {
            // the cmRequestId is not necessarily unique, only process marked permission requests
            if (permissionRequest.status() != PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION) {
                continue;
            }

            var permissionId = permissionRequest.permissionId();
            for (Integer statusCode : cmRequestStatus.consentData().getFirst().responseCodes()) {
                if (statusCode == ResponseCode.CmRevSP.INVALID_PROCESSDATE) {
                    LOGGER.error(
                            "Received a message that indicates that we send an invalid process date for the CMRevoke message for permission request '{}', this should never happen",
                            permissionId);
                } else if (statusCode == ResponseCode.CmRevSP.CONSENT_AND_METERINGPOINT_DO_NOT_MATCH) {
                    LOGGER.error(
                            "Received a message that indicates that the consent id and metering point id provided in the CMRevoke message do not match for permission request '{}', this should never happen",
                            permissionId);
                }
            }

            outbox.commit(
                    new EdaAnswerEvent(permissionId,
                                       PermissionProcessStatus.EXTERNALLY_TERMINATED,
                                       cmRequestStatus.message())
            );
        }
    }

    public void handleCCMSAnswer(CMRequestStatus cmRequestStatus) {
        for (var permissionRequest : repository.findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(),
                                                                                                  cmRequestStatus.cmRequestId())
                .stream().map(EdaPermissionRequest::fromProjection).toList()) {
            // the cmRequestId is not necessarily unique, only process permission requests that have been marked for external termination
            if (permissionRequest.status() == PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION) {
                outbox.commit(
                        new EdaAnswerEvent(permissionRequest.permissionId(),
                                           PermissionProcessStatus.EXTERNALLY_TERMINATED,
                                           cmRequestStatus.message())
                );
            }
        }
    }
}
