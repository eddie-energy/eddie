package energy.eddie.regionconnector.us.green.button.oauth.request;

import energy.eddie.regionconnector.us.green.button.oauth.enums.OAuthGrantType;
import org.springframework.http.client.MultipartBodyBuilder;

public class AccessTokenWithRefreshTokenRequest extends OAuthTokenRequest {
    private final String refreshToken;

    public AccessTokenWithRefreshTokenRequest(String refreshToken) {
        super(OAuthGrantType.REFRESH_TOKEN);
        this.refreshToken = refreshToken;
    }

    @Override
    public MultipartBodyBuilder assembleMultipartBodyBuilder(MultipartBodyBuilder multipartBodyBuilder) {
        multipartBodyBuilder.part("grant_type", getGrantType().getGrantType());
        multipartBodyBuilder.part("refresh_token", refreshToken);

        return multipartBodyBuilder;
    }
}
