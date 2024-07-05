package energy.eddie.regionconnector.fr.enedis.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class FrUsagePointTypeEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @Column(name = "usage_point_type")
    @Enumerated(EnumType.STRING)
    private UsagePointType usagePointType;

    public FrUsagePointTypeEvent(String permissionId, UsagePointType usagePointType) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.usagePointType = usagePointType;
    }

    protected FrUsagePointTypeEvent() {
        super();
        this.usagePointType = null;
    }

    public UsagePointType usagePointType() {
        return usagePointType;
    }
}
