package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
public class UsStartPollingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    public UsStartPollingEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
    }

    protected UsStartPollingEvent() {
        super();
    }
}
