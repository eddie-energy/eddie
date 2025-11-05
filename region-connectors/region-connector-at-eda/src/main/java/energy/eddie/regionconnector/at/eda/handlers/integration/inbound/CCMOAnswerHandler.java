package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CCMOAnswerHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CCMOAnswerHandler.class);
    private final Outbox outbox;
    private final AtPermissionRequestRepository repository;

    public CCMOAnswerHandler(Outbox outbox, AtPermissionRequestRepository repository) {
        this.outbox = outbox;
        this.repository = repository;
    }


    public void handleCCMOAnswer(CMRequestStatus cmRequestStatus) {
        var permissionRequests = repository.findByConversationIdOrCMRequestId(
                                                   cmRequestStatus.conversationId(),
                                                   cmRequestStatus.cmRequestId()
                                           )
                                           .stream()
                                           .map(EdaPermissionRequest::fromProjection)
                                           .toList();
        for (var request : permissionRequests) {
            if (request.status() != PermissionProcessStatus.VALIDATED) {
                LOGGER.atError()
                      .addArgument(request::permissionId)
                      .addArgument(request::conversationId)
                      .addArgument(request::cmRequestId)
                      .log("Received a CCMO_ANSWER message for a permission request that is not in the VALIDATED state. Permission id: '{}', conversation id: '{}', cm request id: '{}'");
                continue;
            }
            outbox.commit(
                    new EdaAnswerEvent(request.permissionId(),
                                       PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                       cmRequestStatus.message())
            );
        }
    }
}
