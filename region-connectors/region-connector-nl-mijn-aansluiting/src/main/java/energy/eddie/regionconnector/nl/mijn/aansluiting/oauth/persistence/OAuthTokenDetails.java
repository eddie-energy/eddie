package energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.persistence;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Clock;
import java.time.Instant;

@Entity
@Table(schema = "nl_mijn_aansluiting")
@SuppressWarnings({"NullAway", "unused"})
public class OAuthTokenDetails {
    @Id
    @Column(length = 36)
    private final String permissionId;
    @Column(columnDefinition = "text")
    private final String accessTokenValue;
    private final Instant accessTokenIssuedAt;
    private final Instant accessTokenExpiresAt;
    @Nullable
    @Column(columnDefinition = "text")
    private final String refreshTokenValue;
    @Nullable
    private final Instant refreshTokenIssuedAt;
    @CreationTimestamp
    private final Instant createdAt;

    protected OAuthTokenDetails() {
        permissionId = null;
        accessTokenValue = null;
        accessTokenIssuedAt = null;
        accessTokenExpiresAt = null;
        refreshTokenValue = null;
        refreshTokenIssuedAt = null;
        createdAt = null;
    }

    public OAuthTokenDetails(
            String permissionId,
            String accessTokenValue,
            Instant accessTokenIssuedAt,
            Instant accessTokenExpiresAt,
            @Nullable String refreshTokenValue,
            @Nullable Instant refreshTokenIssuedAt
    ) {
        this.permissionId = permissionId;
        this.accessTokenValue = accessTokenValue;
        this.accessTokenIssuedAt = accessTokenIssuedAt;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenValue = refreshTokenValue;
        this.refreshTokenIssuedAt = refreshTokenIssuedAt;
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
        return accessTokenExpiresAt.isAfter(now);
    }
}
