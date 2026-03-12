package energy.eddie.regionconnector.de.eta.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Response DTO for ETA Plus Auth token exchange.
 */
public record AuthTokenResponse(
        @Nullable @JsonProperty("data") TokenData data,
        @JsonProperty("success") boolean success) {
    public record TokenData(
            @JsonProperty("token") String token,
            @Nullable @JsonProperty("refreshToken") String refreshToken) {
    }

    public @Nullable String getAccessToken() {
        return data != null ? data.token() : null;
    }

    public @Nullable String getRefreshToken() {
        return data != null ? data.refreshToken() : null;
    }
}
