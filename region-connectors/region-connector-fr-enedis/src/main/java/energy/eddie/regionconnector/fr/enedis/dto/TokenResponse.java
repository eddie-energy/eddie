package energy.eddie.regionconnector.fr.enedis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("expires_in")
        int expires_in,
        @JsonProperty("scope")
        String scope
) {
}