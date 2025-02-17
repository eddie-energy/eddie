package energy.eddie.regionconnector.cds.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.ZonedDateTime;

@Entity(name = "CdsSentToPaEvent")
public class SentToPaEvent extends PersistablePermissionEvent {
    @Column(name = "auth_expires_at")
    private final ZonedDateTime authExpiresAt;
    @Column(name = "state")
    private final String state;

    public SentToPaEvent(String permissionId, ZonedDateTime authExpiresAt, String state) {
        super(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        this.authExpiresAt = authExpiresAt;
        this.state = state;
    }

    @SuppressWarnings("NullAway")
    protected SentToPaEvent() {
        super();
        authExpiresAt = null;
        state = null;
    }

    public ZonedDateTime authExpiresAt() {
        return authExpiresAt;
    }

    public String state() {
        return state;
    }
}
