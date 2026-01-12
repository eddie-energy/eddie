package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

/**
 * Event emitted when a permission request is accepted.
 * This event triggers the start of data polling for the permission request.
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

