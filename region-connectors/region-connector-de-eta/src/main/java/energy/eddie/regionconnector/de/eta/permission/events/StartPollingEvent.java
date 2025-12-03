package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "StartPollingEvent")
public class StartPollingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {

    public StartPollingEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
    }

    public StartPollingEvent() {
        super();
    }
}