package energy.eddie.regionconnector.si.moj.elektro.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "SiSimpleEvent")
@SuppressWarnings({"NullAway", "unused"})
public class SimpleEvent extends PersistablePermissionEvent {
    public SimpleEvent(String permissionId, PermissionProcessStatus status) {
        super(permissionId, status);
    }

    protected SimpleEvent() {
    }
}
