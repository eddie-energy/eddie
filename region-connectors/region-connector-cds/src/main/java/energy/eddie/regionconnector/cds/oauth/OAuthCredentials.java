// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.oauth;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity(name = "CdsOAuthCredentials")
@Table(name = "oauth_credentials", schema = "cds")
public class OAuthCredentials {
    @Id
    @Column(name = "permission_id", length = 36, nullable = false)
    @SuppressWarnings("unused")
    private final String permissionId;
    @Column(name = "refresh_token")
    @Nullable
    private String refreshToken;
    @Column(name = "access_token", columnDefinition = "text")
    @Nullable
    private String accessToken;
    @Column(name = "expires_at")
    @Nullable
    private ZonedDateTime expiresAt;

    public OAuthCredentials(
            String permissionId,
            @Nullable String refreshToken,
            @Nullable String accessToken,
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

    public String permissionId() {
        return permissionId;
    }

    @Nullable
    public String refreshToken() {
        return refreshToken;
    }

    @Nullable
    public String accessToken() {
        return accessToken;
    }

    public boolean isValid(ZonedDateTime zonedDateTime) {
        return accessToken != null && expiresAt != null && expiresAt.isAfter(zonedDateTime);
    }

    public boolean isValid() {
        return isValid(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public OAuthCredentials updateAccessToken(String accessToken, ZonedDateTime expiresAt) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        return this;
    }

    public OAuthCredentials updateAllTokens(String refreshToken, String accessToken, ZonedDateTime expiresAt) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        return this;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(permissionId);
        result = 31 * result + Objects.hashCode(refreshToken);
        result = 31 * result + Objects.hashCode(accessToken);
        result = 31 * result + Objects.hashCode(expiresAt);
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof OAuthCredentials that)) return false;

        return Objects.equals(permissionId, that.permissionId)
               && Objects.equals(refreshToken, that.refreshToken)
               && Objects.equals(accessToken, that.accessToken)
               && Objects.equals(expiresAt, that.expiresAt);
    }
}
