package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaAcceptedEvent")
public class AcceptedEvent extends PersistablePermissionEvent {
    public AcceptedEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
    }

    protected AcceptedEvent() {
        super();
    }
}
