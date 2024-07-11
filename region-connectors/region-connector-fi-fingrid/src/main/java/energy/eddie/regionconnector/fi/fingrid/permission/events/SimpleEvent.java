package energy.eddie.regionconnector.fi.fingrid.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

import java.time.Clock;

@Entity(name = "FiSimpleEvent")
public class SimpleEvent extends PersistablePermissionEvent {
    public SimpleEvent(String permissionId, PermissionProcessStatus status) {
        super(permissionId, status);
    }

    public SimpleEvent(String permissionId, PermissionProcessStatus status, Clock clock) {
        super(permissionId, status, clock);
    }

    public SimpleEvent() {

    }
}
