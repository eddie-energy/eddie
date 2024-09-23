package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings("NullAway")
public class UsAcceptedEvent extends PersistablePermissionEvent {
    private final String authUid;

    public UsAcceptedEvent(String permissionId, String authUid) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.authUid = authUid;
    }

    protected UsAcceptedEvent() {
        this.authUid = null;
    }

    public String authUid() {
        return authUid;
    }
}
