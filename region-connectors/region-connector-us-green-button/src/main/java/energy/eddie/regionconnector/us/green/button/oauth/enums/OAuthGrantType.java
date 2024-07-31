package energy.eddie.regionconnector.us.green.button.oauth.enums;

public enum OAuthGrantType {
    CLIENT_CREDENTIALS("client_credentials"),
    AUTHORIZATION_CODE("authorization_code"),
    REFRESH_TOKEN("refresh_token");

    private final String grantType;

    OAuthGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getGrantType() {
        return grantType;
    }
}
