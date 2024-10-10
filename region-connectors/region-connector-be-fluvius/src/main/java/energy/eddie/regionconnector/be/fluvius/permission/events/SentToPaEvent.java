package energy.eddie.regionconnector.be.fluvius.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class SentToPaEvent extends PersistablePermissionEvent {
    private final String shortUrlIdentifier;

    public SentToPaEvent(String permissionId, String shortUrlIdentifier) {
        super(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        this.shortUrlIdentifier = shortUrlIdentifier;
    }

    protected SentToPaEvent() {
        shortUrlIdentifier = null;
    }
}
