package energy.eddie.regionconnector.at.api;

import jakarta.annotation.Nullable;

import java.util.Optional;

/**
 * The repository for PermissionRequests.
 * It saves permission requests and can query with the permission id.
 */
public interface PermissionRequestRepository {
    /**
     * Save the permission request.
     * If a request with the same permission id already exists it will be overwritten.
     *
     * @param request the permission request to be saved.
     */
    void save(PermissionRequest request);

    /**
     * Finds a permission request by its permission id.
     * If there is no permission request it returns an empty optional.
     *
     * @param permissionId the id of the permission request.
     * @return an optional that contains the permission request if it exists.
     */
    Optional<PermissionRequest> findByPermissionId(String permissionId);

    /**
     * Finds a permission request by either its conversation id or its CMRequest id.
     *
     * @param conversationId the conversation id of the request.
     * @param cmRequestId    the request id of the original CM Request.
     * @return an optional, which is empty if there is no matching permission request.
     */
    Optional<PermissionRequest> findByConversationIdOrCMRequestId(String conversationId, @Nullable String cmRequestId);

    /**
     * Removes a permission request by its permission id.
     *
     * @param permissionId the permission id of the request to delete.
     * @return true if the permission request was deleted, false if not.
     */
    boolean removeByPermissionId(String permissionId);
}
