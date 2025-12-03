package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

import java.time.ZonedDateTime;

@Entity(name = "DeEtaCreatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class CreatedEvent extends PersistablePermissionEvent {

    public CreatedEvent(String permissionId, String dataNeedId, String connectionId) {
        super(permissionId, PermissionProcessStatus.CREATED, connectionId, dataNeedId);
    }

    public CreatedEvent(String permissionId, String dataNeedId, String connectionId, ZonedDateTime created) {
        super(permissionId, PermissionProcessStatus.CREATED, connectionId, dataNeedId, created);
    }

    protected CreatedEvent() {
        super();
    }
}
