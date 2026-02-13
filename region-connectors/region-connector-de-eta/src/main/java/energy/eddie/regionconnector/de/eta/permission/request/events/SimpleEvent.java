package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

/**
 * Simple permission event for the German (DE) region connector.
 * This event is used for basic status changes.
 */
@Entity(name = "DeSimpleEvent")
@SuppressWarnings("NullAway") // Needed for JPA
public class SimpleEvent extends PersistablePermissionEvent {
    
    public SimpleEvent(String permissionId, PermissionProcessStatus status) {
        super(permissionId, status);
    }

    protected SimpleEvent() {
        super();
    }
}
