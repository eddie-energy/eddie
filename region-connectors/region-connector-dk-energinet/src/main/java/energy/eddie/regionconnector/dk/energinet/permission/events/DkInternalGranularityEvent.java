package energy.eddie.regionconnector.dk.energinet.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class DkInternalGranularityEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @Enumerated(EnumType.STRING)
    private final Granularity granularity;

    public DkInternalGranularityEvent(String permissionId, Granularity granularity) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.granularity = granularity;
    }

    protected DkInternalGranularityEvent() {
        granularity = null;
    }

    public Granularity granularity() {
        return granularity;
    }
}
