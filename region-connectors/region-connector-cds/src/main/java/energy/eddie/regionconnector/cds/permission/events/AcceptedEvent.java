package energy.eddie.regionconnector.cds.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "CdsAcceptedEvent")
public class AcceptedEvent extends PersistablePermissionEvent{
    public AcceptedEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
    }

    protected AcceptedEvent() {
        super();
    }
}
