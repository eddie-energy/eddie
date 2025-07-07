package energy.eddie.regionconnector.fi.fingrid.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "FiAcceptedEvent")
public class AcceptedEvent extends PersistablePermissionEvent {
    public AcceptedEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
    }

    protected AcceptedEvent() {}
}
