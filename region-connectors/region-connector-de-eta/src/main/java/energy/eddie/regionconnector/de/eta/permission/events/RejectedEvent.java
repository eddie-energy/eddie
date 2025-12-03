package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaRejectedEvent")
public class RejectedEvent extends PersistablePermissionEvent {
    public RejectedEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.REJECTED);
    }

    protected RejectedEvent() {
        super();
    }
}
