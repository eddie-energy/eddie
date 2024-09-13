package energy.eddie.regionconnector.us.green.button.oauth.persistence;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Clock;
import java.time.Instant;

@Entity(name = "oauth_token_details")
@Table(schema = "us_green_button")
@SuppressWarnings({"NullAway", "unused"})
public class OAuthTokenDetails {
    @Id
    @Column(length = 36)
    private final String permissionId;
    @Column(columnDefinition = "text")
    private final String accessTokenValue;
    private final Instant accessTokenIssuedAt;
    private final Instant accessTokenExpiresAt;
    @Column(columnDefinition = "text")
    private final String refreshTokenValue;
    @CreationTimestamp
    private final Instant createdAt;
    private final String authUid;

    protected OAuthTokenDetails() {
        permissionId = null;
        accessTokenValue = null;
        accessTokenIssuedAt = null;
        accessTokenExpiresAt = null;
        refreshTokenValue = null;
        createdAt = null;
        authUid = null;
    }

    public OAuthTokenDetails(
            String permissionId,
            String accessTokenValue,
            Instant accessTokenIssuedAt,
            Instant accessTokenExpiresAt,
            String refreshTokenValue,
            String authUid
    ) {
        this.permissionId = permissionId;
        this.accessTokenValue = accessTokenValue;
        this.accessTokenIssuedAt = accessTokenIssuedAt;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenValue = refreshTokenValue;
        this.authUid = authUid;
        this.createdAt = null;
    }

    @Nullable
    public String refreshToken() {
        return refreshTokenValue;
    }

    public String accessToken() {
        return accessTokenValue;
    }

    public boolean isValid() {
        Instant now = Instant.now(Clock.systemUTC());
        return accessTokenExpiresAt != null && accessTokenExpiresAt.isAfter(now);
    }

    public String authUid() {
        return authUid;
    }
}