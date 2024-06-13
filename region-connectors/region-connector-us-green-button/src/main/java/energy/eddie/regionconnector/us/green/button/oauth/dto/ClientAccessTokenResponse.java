package energy.eddie.regionconnector.us.green.button.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("NullAway")
public class ClientAccessTokenResponse {
    /*
        "token_type": "Bearer",
        "access_token": "47ff9a6285d24b07a6390b9a705993b176b7bed3c8d844f6a93b3f0a7d475ca9",
        "expires_in": 3600,
        "scope": "FB=1_3_14_32",
        "resourceURI": "https://utilityapi.com/DataCustodian/espi/1_1/resource/Bulk/1111",
        "authorizationURI": "https://utilityapi.com/DataCustodian/espi/1_1/resource/Authorization/1111"
     */
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("expires_in")
    private int expiresIn;
    @JsonProperty("scope")
    private String scope;
    @JsonProperty("resourceURI")
    private String resourceUri;
    @JsonProperty("authorizationURI")
    private String authorizationUri;

    public String getTokenType() {
        return tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }
}
