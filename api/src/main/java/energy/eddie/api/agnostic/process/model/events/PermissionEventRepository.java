package energy.eddie.api.agnostic.process.model.events;

public interface PermissionEventRepository {
    /**
     * Saves a {@link PermissionEvent} and flushes changes instantly.
     *
     * @param permissionEvent permissionEvent to be saved. Must not be null.
     * @return the saved permissionEvent.
     */
    PermissionEvent saveAndFlush(PermissionEvent permissionEvent);
}
