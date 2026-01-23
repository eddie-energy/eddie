// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.process.model.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequest;

import java.util.Optional;

public interface PermissionRequestRepository<T extends PermissionRequest> {
    /**
     * Save the permission request. If a request with the same permission id already exists it will be overwritten.
     *
     * @param request the permission request to be saved.
     */
    void save(T request);

    /**
     * Finds a permission request by its permission id. If there is no permission request, it returns an empty optional.
     *
     * @param permissionId the id of the permission request.
     * @return an optional that contains the permission request if it exists.
     */
    Optional<T> findByPermissionId(String permissionId);

    /**
     * Gets a permission request by its permission id. If it cannot be found throws {@code EntityNotFoundException}.
     * Should only be used when sure that the permission request exists
     *
     * @param permissionId the id of the permission request.
     * @return the permission request
     */
    T getByPermissionId(String permissionId);
}