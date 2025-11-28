package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaSentToPaEvent")
@SuppressWarnings({"NullAway", "unused"})
public class SentToPaEvent extends PersistablePermissionEvent {
    public SentToPaEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
    }

    protected SentToPaEvent() {}
}
