package energy.eddie.regionconnector.us.green.button.oauth.request;

import energy.eddie.regionconnector.us.green.button.oauth.enums.OAuthGrantType;
import org.springframework.http.client.MultipartBodyBuilder;

public class AccessTokenWithCodeRequest extends OAuthTokenRequest {
    private final String code;
    private final String redirectUri;

    public AccessTokenWithCodeRequest(String code, String redirectUri) {
        super(OAuthGrantType.AUTHORIZATION_CODE);
        this.code = code;
        this.redirectUri = redirectUri;
    }

    @Override
    public MultipartBodyBuilder assembleMultipartBodyBuilder(MultipartBodyBuilder multipartBodyBuilder) {
        multipartBodyBuilder.part("grant_type", getGrantType().getGrantType());
        multipartBodyBuilder.part("code", code);
        multipartBodyBuilder.part("redirect_uri", redirectUri);

        return multipartBodyBuilder;
    }
}
