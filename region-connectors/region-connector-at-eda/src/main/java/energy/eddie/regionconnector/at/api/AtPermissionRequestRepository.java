package energy.eddie.regionconnector.at.api;

import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import jakarta.annotation.Nullable;

import java.util.Optional;

/**
 * The repository for PermissionRequests.
 * It saves permission requests and can query with the permission id.
 */
public interface AtPermissionRequestRepository extends PermissionRequestRepository<AtPermissionRequest> {

    /**
     * Finds a permission request by either its conversation id or its CMRequest id.
     *
     * @param conversationId the conversation id of the request.
     * @param cmRequestId    the request id of the original CM Request.
     * @return an optional, which is empty if there is no matching permission request.
     */
    Optional<AtPermissionRequest> findByConversationIdOrCMRequestId(String conversationId, @Nullable String cmRequestId);

}
