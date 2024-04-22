package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.PermissionRequest;

import java.time.LocalDate;

public interface FulfillmentService {
    /**
     * Checks if the permission request is fulfilled by the given date.
     *
     * @param permissionRequest the permission request
     * @param date              the date to check against
     * @return true if {@code date} is after {@link PermissionRequest#end()}
     */
    default boolean isPermissionRequestFulfilledByDate(PermissionRequest permissionRequest, LocalDate date) {
        return date.isAfter(permissionRequest.end());
    }

    /**
     * Tries to fulfill the permission request.
     *
     * @param permissionRequest the permission request
     */
    void tryFulfillPermissionRequest(PermissionRequest permissionRequest);
}
