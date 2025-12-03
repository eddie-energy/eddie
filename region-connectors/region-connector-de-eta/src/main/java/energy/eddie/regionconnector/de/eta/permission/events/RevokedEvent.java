package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaRevokedEvent")
public class RevokedEvent extends PersistablePermissionEvent {
    public RevokedEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.REVOKED);
    }

    protected RevokedEvent() {
        super();
    }
}
