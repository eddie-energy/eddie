package energy.eddie.regionconnector.fr.enedis.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class FrAcceptedEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String usagePointId;

    public FrAcceptedEvent(String permissionId, String usagePointId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.usagePointId = usagePointId;
    }

    protected FrAcceptedEvent() {
        super();
        this.usagePointId = null;
    }

    public String usagePointId() {
        return usagePointId;
    }
}
