package energy.eddie.regionconnector.be.fluvius.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
public class MeterReadingUpdatedEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    public MeterReadingUpdatedEvent(String permissionId, PermissionProcessStatus status) {
        super(permissionId, status);
    }

    protected MeterReadingUpdatedEvent() {
        super();
    }
}
