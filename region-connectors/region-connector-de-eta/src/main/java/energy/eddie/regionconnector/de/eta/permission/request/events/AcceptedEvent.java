package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

/**
 * Event emitted when a permission request is accepted by the ETA Plus system.
 * This indicates that the final customer has granted permission for data access.
 */
@Entity(name = "DeAcceptedEvent")
@SuppressWarnings("NullAway")
public class AcceptedEvent extends PersistablePermissionEvent {

    public AcceptedEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
    }

    protected AcceptedEvent() {
        super();
    }
}

