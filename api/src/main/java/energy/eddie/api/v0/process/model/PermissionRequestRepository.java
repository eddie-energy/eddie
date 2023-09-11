package energy.eddie.api.v0.process.model;

import java.util.Optional;

public interface PermissionRequestRepository<T extends PermissionRequest> {
    /**
     * Save the permission request.
     * If a request with the same permission id already exists it will be overwritten.
     *
     * @param request the permission request to be saved.
     */
    void save(T request);

    /**
     * Finds a permission request by its permission id.
     * If there is no permission request it returns an empty optional.
     *
     * @param permissionId the id of the permission request.
     * @return an optional that contains the permission request if it exists.
     */
    Optional<T> findByPermissionId(String permissionId);

    /**
     * Removes a permission request by its permission id.
     *
     * @param permissionId the permission id of the request to delete.
     * @return true if the permission request was deleted, false if not.
     */
    boolean removeByPermissionId(String permissionId);
}
