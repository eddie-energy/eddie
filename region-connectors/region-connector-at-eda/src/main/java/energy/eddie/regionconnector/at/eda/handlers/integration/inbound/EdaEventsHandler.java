package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
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

    public EdaEventsHandler(EdaAdapter edaAdapter, Outbox outbox, AtPermissionRequestRepository repository) {
        this.outbox = outbox;
        this.repository = repository;
        edaAdapter.getCMRequestStatusStream()
                  .subscribe(this::processIncomingCmStatusMessages);
    }

    /**
     * Process a CMRequestStatus and emit a ConnectionStatusMessage if possible, also adds connectionId and permissionId
     * for identification
     *
     * @param cmRequestStatus the CMRequestStatus to process
     */
    private void processIncomingCmStatusMessages(CMRequestStatus cmRequestStatus) {
        var optionalPermissionRequest = repository.findByConversationIdOrCMRequestId(
                cmRequestStatus.getConversationId(),
                cmRequestStatus.getCMRequestId().orElse(null)
        );
        if (optionalPermissionRequest.isEmpty()) {
            // should not happen if a persistent mapping is used.
            LOGGER.error("Received CMRequestStatus for unknown conversationId {} or requestId {} with payload: {}",
                         cmRequestStatus.getConversationId(), cmRequestStatus.getCMRequestId(), cmRequestStatus);
            return;
        }
        transitionPermissionRequest(cmRequestStatus, optionalPermissionRequest.get());
    }

    private void transitionPermissionRequest(CMRequestStatus cmRequestStatus, AtPermissionRequest request) {
        String permissionId = request.permissionId();
        switch (cmRequestStatus.getStatus()) {
            case ACCEPTED -> {
                Optional<String> meteringPoint = cmRequestStatus.getMeteringPoint();
                if (meteringPoint.isEmpty()) {
                    LOGGER.error(
                            "Metering point id is missing in ACCEPTED CMRequestStatus message for permission id: {} ",
                            permissionId);
                    return;
                }

                Optional<String> cmConsentId = cmRequestStatus.getCMConsentId();
                if (cmConsentId.isEmpty()) {
                    LOGGER.error("Got accept message without consent id for permission request with permission id {}",
                                 permissionId);
                    return;
                }
                outbox.commit(new AcceptedEvent(
                        permissionId,
                        meteringPoint.get(),
                        cmConsentId.get(),
                        cmRequestStatus.getMessage()
                ));
            }
            case ERROR -> {
                // If the DSO does not exist EDA will respond with an error without sending a received-message.
                // In that case the error message is an implicit received-message.
                if (request.status() == PermissionProcessStatus.VALIDATED) {
                    outbox.commit(
                            new EdaAnswerEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                               cmRequestStatus.getMessage()));
                }
                outbox.commit(
                        new EdaAnswerEvent(permissionId, PermissionProcessStatus.INVALID, cmRequestStatus.getMessage())
                );
            }
            case REJECTED -> outbox.commit(
                    new EdaAnswerEvent(permissionId, PermissionProcessStatus.REJECTED, cmRequestStatus.getMessage()));
            case RECEIVED -> outbox.commit(
                    new EdaAnswerEvent(permissionId,
                                       PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                       cmRequestStatus.getMessage())
            );
            default -> {
                // Other CMRequestStatus do not change the state of the permission request,
                // because they have no matching state in the consent process model
            }
        }
    }
}
