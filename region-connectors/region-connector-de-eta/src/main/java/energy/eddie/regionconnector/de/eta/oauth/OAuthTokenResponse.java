package energy.eddie.regionconnector.de.eta.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Response DTO for ETA Plus OAuth token exchange.
 */
public record OAuthTokenResponse(
        @JsonProperty("data") TokenData data,
        @JsonProperty("success") boolean success) {
    public record TokenData(
            @JsonProperty("token") String token,
            @JsonProperty("refreshToken") String refreshToken) {
    }

    public @Nullable String getAccessToken() {
        return data != null ? data.token() : null;
    }

    public @Nullable String getRefreshToken() {
        return data != null ? data.refreshToken() : null;
    }
}
