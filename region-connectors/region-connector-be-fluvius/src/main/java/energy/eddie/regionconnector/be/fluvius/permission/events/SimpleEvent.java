package energy.eddie.regionconnector.be.fluvius.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "BeSimpleEvent")
public class SimpleEvent extends PersistablePermissionEvent {
    public SimpleEvent(String permissionId, PermissionProcessStatus status) {
        super(permissionId, status);
    }

    protected SimpleEvent() {
    }
}
