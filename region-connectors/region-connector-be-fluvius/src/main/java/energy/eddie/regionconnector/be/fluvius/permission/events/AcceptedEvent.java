package energy.eddie.regionconnector.be.fluvius.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity(name = "BeAcceptedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class AcceptedEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String eanNumber;

    public AcceptedEvent(String permissionId, String eanNumber) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.eanNumber = eanNumber;
    }

    public AcceptedEvent() {
        this.eanNumber = null;
    }
}
