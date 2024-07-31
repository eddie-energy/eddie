package energy.eddie.regionconnector.us.green.button.oauth.request;

import energy.eddie.regionconnector.us.green.button.oauth.enums.OAuthGrantType;
import org.springframework.http.client.MultipartBodyBuilder;

public abstract class OAuthTokenRequest {
    private final OAuthGrantType grantType;

    protected OAuthTokenRequest(OAuthGrantType grantType) {
        this.grantType = grantType;
    }

    public OAuthGrantType getGrantType() {
        return grantType;
    }

    public abstract MultipartBodyBuilder assembleMultipartBodyBuilder(MultipartBodyBuilder multipartBodyBuilder);
}
