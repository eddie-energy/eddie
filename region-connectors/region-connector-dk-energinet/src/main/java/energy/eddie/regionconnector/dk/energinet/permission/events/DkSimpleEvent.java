package energy.eddie.regionconnector.dk.energinet.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class DkSimpleEvent extends PersistablePermissionEvent {
    public DkSimpleEvent(String permissionId, PermissionProcessStatus status) {
        super(permissionId, status);
    }

    protected DkSimpleEvent() {}
}
