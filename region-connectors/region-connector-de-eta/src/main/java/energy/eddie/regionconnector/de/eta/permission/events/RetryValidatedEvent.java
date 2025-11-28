package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaRetryValidatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class RetryValidatedEvent extends PersistablePermissionEvent {
    public RetryValidatedEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
    }

    protected RetryValidatedEvent() {}
}
