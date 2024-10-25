package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings("NullAway")
public class UsUnfulfillableEvent extends PersistablePermissionEvent {
    private final Boolean requiresExternalTermination;

    public UsUnfulfillableEvent(String permissionId, boolean requiresExternalTermination) {
        super(permissionId, PermissionProcessStatus.UNFULFILLABLE);
        this.requiresExternalTermination = requiresExternalTermination;
    }

    protected UsUnfulfillableEvent() {
        requiresExternalTermination = null;
    }

    public boolean requiresExternalTermination() {
        return Boolean.TRUE.equals(requiresExternalTermination);
    }
}
