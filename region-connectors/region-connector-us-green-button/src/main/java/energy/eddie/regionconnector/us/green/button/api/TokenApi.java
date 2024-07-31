package energy.eddie.regionconnector.us.green.button.api;

import energy.eddie.regionconnector.us.green.button.oauth.dto.AccessTokenResponse;
import energy.eddie.regionconnector.us.green.button.oauth.dto.ClientAccessTokenResponse;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithCodeRequest;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithRefreshTokenRequest;
import energy.eddie.regionconnector.us.green.button.oauth.request.ClientAccessTokenRequest;
import reactor.core.publisher.Mono;

public interface TokenApi {
    // This type of access token lets you access Resource Endpoints and Batch Endpoints (e.g. the data shared by a individual authorization).
    Mono<AccessTokenResponse> accessToken(AccessTokenWithCodeRequest tokenRequest);

    Mono<AccessTokenResponse> accessToken(AccessTokenWithRefreshTokenRequest tokenRequest);

    // This type of access token lets you access Authorization Endpoints and Bulk Endpoints (e.g. your list of authorizations and bulk payloads from notifications).
    Mono<ClientAccessTokenResponse> clientAccessToken(ClientAccessTokenRequest tokenRequest);
}
