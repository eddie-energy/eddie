package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.stereotype.Component;

@Component
public class PontonErrorHandler {

    private final Outbox outbox;
    private final AtPermissionRequestRepository repository;

    public PontonErrorHandler(Outbox outbox, AtPermissionRequestRepository repository) {
        this.outbox = outbox;
        this.repository = repository;
    }


    public void handlePontonError(CMRequestStatus cmRequestStatus) {
        // If the DSO does not exist EDA will respond with an error without sending a received-message.
        // In that case the error message is an implicit received-message.
        for (var request : repository
                .findByConversationIdOrCMRequestId(
                    cmRequestStatus.conversationId(),
                    cmRequestStatus.cmRequestId())
                .stream()
                .map(EdaPermissionRequest::fromProjection)
                .toList()) {
            if (request.status() == PermissionProcessStatus.VALIDATED) {
                outbox.commit(
                        new EdaAnswerEvent(request.permissionId(),
                                           PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                           cmRequestStatus.message()));
            }

            outbox.commit(
                    new EdaAnswerEvent(request.permissionId(),
                                       PermissionProcessStatus.INVALID,
                                       cmRequestStatus.message())
            );
        }
    }
}
