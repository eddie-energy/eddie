package energy.eddie.regionconnector.us.green.button.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
    "token_type": "Bearer",
    "access_token": "64999645a0b5449b871ad0333df6cb114415c9a5522d41118a0f7939dd3f0208",
    "refresh_token": "76fd81fd6a3b42a592eed9ff9b8d5cfda9d53324919643f4a45fe34d664442f9",
    "expires_in": 3600,
    "scope": "FB=1_3_4_5_8_13_14_18_19_34_35_39_51;IntervalDuration=900_3600;BlockDuration=daily;HistoryLength=34128000;SubscriptionFrequency=daily;AccountCollection=2",
    "resourceURI": "https://utilityapi.com/DataCustodian/espi/1_1/resource/Subscription/1111",
    "customerResourceURI": "https://utilityapi.com/DataCustodian/espi/1_1/resource/RetailCustomer/1111",
    "authorizationURI": "https://utilityapi.com/DataCustodian/espi/1_1/resource/Authorization/1111",
 */
@SuppressWarnings("NullAway")
public record AccessTokenResponse(
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("refresh_token")
        String refreshToken,
        @JsonProperty("expires_in")
        int expiresIn,
        @JsonProperty("scope")
        String scope,
        @JsonProperty("resourceURI")
        String resourceUri,
        @JsonProperty("customerResourceURI")
        String customerResourceUri,
        @JsonProperty("authorizationURI")
        String authorizationUri) {}
