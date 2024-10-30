package energy.eddie.api.agnostic.process.model.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequest;

import java.util.Collection;

@FunctionalInterface
public interface StalePermissionRequestRepository<T extends PermissionRequest> {

    /**
     * Finds all permission request that are older than the {@code stalenessDuration} and have either the status {@code VALIDATED} or {@code SENT_TO_PERMISSION_ADMINISTRATOR}.
     *
     * @param stalenessDuration the max amount of hours the permission request has to be created ago to not be considered stale.
     * @return all permission request created now - {@code stalenessDuration} hours ago, with above specified status.
     */
    Collection<T> findStalePermissionRequests(int stalenessDuration);
}
