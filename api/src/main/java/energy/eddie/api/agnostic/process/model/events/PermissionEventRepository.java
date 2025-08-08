package energy.eddie.api.agnostic.process.model.events;


import java.time.ZonedDateTime;
import java.util.List;

public interface PermissionEventRepository {
    /**
     * Saves a {@link PermissionEvent} and flushes changes instantly.
     *
     * @param permissionEvent permissionEvent to be saved. Must not be null.
     * @return the saved permissionEvent.
     */
    PermissionEvent saveAndFlush(PermissionEvent permissionEvent);
    List<PermissionEvent> findTop2ByPermissionIdAndEventCreatedLessThanEqualOrderByEventCreatedDesc(String permissionId, ZonedDateTime eventCreated);
}
