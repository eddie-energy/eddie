package energy.eddie.regionconnector.cds.oauth;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity(name = "CdsOAuthCredentials")
@Table(name = "oauth_credentials", schema = "cds")
public class OAuthCredentials {
    @Id
    @Column(name = "permission_id", length = 36, nullable = false)
    private final String permissionId;
    @Column(name = "refresh_token", nullable = false)
    private final String refreshToken;
    @Column(name = "access_token", columnDefinition = "text")
    @Nullable
    private final String accessToken;
    @Column(name = "expires_at")
    @Nullable
    private final ZonedDateTime expiresAt;

    public OAuthCredentials(
            String permissionId, String refreshToken, @Nullable String accessToken,
            @Nullable ZonedDateTime expiresAt
    ) {
        this.permissionId = permissionId;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    @SuppressWarnings("NullAway")
    protected OAuthCredentials() {
        permissionId = null;
        refreshToken = null;
        accessToken = null;
        expiresAt = null;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public String accessToken() {
        return accessToken;
    }

    public boolean isValid(ZonedDateTime zonedDateTime) {
        return accessToken != null && expiresAt != null && expiresAt.isAfter(zonedDateTime);
    }

    public boolean isValid() {
        return isValid(ZonedDateTime.now(ZoneOffset.UTC));
    }
}
