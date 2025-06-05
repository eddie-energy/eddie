package energy.eddie.regionconnector.fr.enedis.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class FrGranularityUpdateEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;

    public FrGranularityUpdateEvent(String permissionId, Granularity granularity) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.granularity = granularity;
    }

    protected FrGranularityUpdateEvent() {
        granularity = null;
    }

    public Granularity granularity() {
        return granularity;
    }
}
