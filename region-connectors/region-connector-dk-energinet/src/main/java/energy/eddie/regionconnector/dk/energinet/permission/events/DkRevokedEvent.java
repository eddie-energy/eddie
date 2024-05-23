package energy.eddie.regionconnector.dk.energinet.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class DkRevokedEvent extends PersistablePermissionEvent {
    @Column(name = "errors", columnDefinition = "text")
    private final String reason;

    public DkRevokedEvent(String permissionId, @Nullable String reason) {
        super(permissionId, PermissionProcessStatus.REVOKED);
        this.reason = reason;
    }

    protected DkRevokedEvent() {
        reason = null;
    }
}
