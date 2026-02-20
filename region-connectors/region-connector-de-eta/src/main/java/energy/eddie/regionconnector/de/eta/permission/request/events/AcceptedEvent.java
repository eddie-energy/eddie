package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Event emitted when a permission request is accepted (OAuth token obtained).
 */
@Entity(name = "DeAcceptedEvent")
@SuppressWarnings({ "NullAway", "unused" })
public class AcceptedEvent extends PersistablePermissionEvent {

    @Column(name = "access_token", columnDefinition = "text")
    private String accessToken;

    @Nullable
    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken;

    public AcceptedEvent(String permissionId, String accessToken, @Nullable String refreshToken) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    protected AcceptedEvent() {
    }

    public String accessToken() {
        return accessToken;
    }

    public java.util.Optional<String> refreshToken() {
        return java.util.Optional.ofNullable(refreshToken);
    }
}
