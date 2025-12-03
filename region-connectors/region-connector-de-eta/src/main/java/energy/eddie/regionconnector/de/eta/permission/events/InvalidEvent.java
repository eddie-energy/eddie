package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaInvalidEvent")
public class InvalidEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text", name = "invalid_message")
    private final String invalidMessage;

    public InvalidEvent(String permissionId, String invalidMessage) {
        super(permissionId, PermissionProcessStatus.INVALID);
        this.invalidMessage = invalidMessage;
    }

    protected InvalidEvent() {
        super();
        this.invalidMessage = null;
    }
}
