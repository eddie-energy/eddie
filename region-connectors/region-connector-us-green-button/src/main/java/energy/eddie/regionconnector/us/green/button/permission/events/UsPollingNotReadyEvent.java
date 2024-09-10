package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@SuppressWarnings("NullAway")
public class UsPollingNotReadyEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @Column(name = "polling_status")
    @Enumerated(EnumType.STRING)
    @SuppressWarnings("unused")
    private final PollingStatus pollingStatus;

    public UsPollingNotReadyEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.pollingStatus = PollingStatus.DATA_NOT_READY;
    }

    protected UsPollingNotReadyEvent() {
        super();
        this.pollingStatus = null;
    }
}
