package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.oauth.dto.AccessTokenResponse;
import energy.eddie.regionconnector.us.green.button.oauth.dto.ClientAccessTokenResponse;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithCodeRequest;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithRefreshTokenRequest;
import energy.eddie.regionconnector.us.green.button.oauth.request.ClientAccessTokenRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.mockito.Mockito.mock;

class OAuthTokenClientTest {

    private OAuthTokenClient oAuthTokenClient;
    private MockWebServer mockWebServer;
    private GreenButtonConfiguration greenButtonConfiguration;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        greenButtonConfiguration = mock(GreenButtonConfiguration.class);
        oAuthTokenClient = new OAuthTokenClient(baseUrl,
                                                "test-client-id",
                                                "test-client-secret",
                                                greenButtonConfiguration);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testAccessTokenWithCodeRequest() {
        AccessTokenWithCodeRequest request = new AccessTokenWithCodeRequest("code", "uri");
        mockWebServer.enqueue(new MockResponse()
                                      .setBody(
                                              "{ \"token_type\": \"Bearer\", \"access_token\": \"test-token\", \"refresh_token\": \"refresh-token\", \"expires_in\": 3600, \"scope\": \"test-scope\", \"resourceURI\": \"https://example.com/resource\", \"customerResourceURI\": \"https://example.com/customer\", \"authorizationURI\": \"https://example.com/authorization\" }")
                                      .addHeader("Content-Type", "application/json"));

        Mono<AccessTokenResponse> response = oAuthTokenClient.accessToken(request);

        StepVerifier.create(response)
                    .expectNextMatches(accessTokenResponse -> "test-token".equals(accessTokenResponse.getAccessToken()) &&
                                                              "Bearer".equals(accessTokenResponse.getTokenType()) &&
                                                              "refresh-token".equals(accessTokenResponse.getRefreshToken()) &&
                                                              3600 == accessTokenResponse.getExpiresIn() &&
                                                              "test-scope".equals(accessTokenResponse.getScope()) &&
                                                              "https://example.com/resource".equals(accessTokenResponse.getResourceUri()) &&
                                                              "https://example.com/customer".equals(accessTokenResponse.getCustomerResourceUri()) &&
                                                              "https://example.com/authorization".equals(
                                                                      accessTokenResponse.getAuthorizationUri()))
                    .verifyComplete();
    }

    @Test
    void testAccessTokenWithRefreshTokenRequest() {
        AccessTokenWithRefreshTokenRequest request = new AccessTokenWithRefreshTokenRequest("refreshToken");
        mockWebServer.enqueue(new MockResponse()
                                      .setBody(
                                              "{ \"token_type\": \"Bearer\", \"access_token\": \"test-token\", \"refresh_token\": \"refresh-token\", \"expires_in\": 3600, \"scope\": \"test-scope\", \"resourceURI\": \"https://example.com/resource\", \"customerResourceURI\": \"https://example.com/customer\", \"authorizationURI\": \"https://example.com/authorization\" }")
                                      .addHeader("Content-Type", "application/json"));

        Mono<AccessTokenResponse> response = oAuthTokenClient.accessToken(request);

        StepVerifier.create(response)
                    .expectNextMatches(accessTokenResponse -> "test-token".equals(accessTokenResponse.getAccessToken()) &&
                                                              "Bearer".equals(accessTokenResponse.getTokenType()) &&
                                                              "refresh-token".equals(accessTokenResponse.getRefreshToken()) &&
                                                              3600 == accessTokenResponse.getExpiresIn() &&
                                                              "test-scope".equals(accessTokenResponse.getScope()) &&
                                                              "https://example.com/resource".equals(accessTokenResponse.getResourceUri()) &&
                                                              "https://example.com/customer".equals(accessTokenResponse.getCustomerResourceUri()) &&
                                                              "https://example.com/authorization".equals(
                                                                      accessTokenResponse.getAuthorizationUri()))
                    .verifyComplete();
    }

    @Test
    void testClientAccessToken() {
        ClientAccessTokenRequest request = new ClientAccessTokenRequest();
        mockWebServer.enqueue(new MockResponse()
                                      .setBody(
                                              "{ \"token_type\": \"Bearer\", \"access_token\": \"client-test-token\", \"expires_in\": 3600, \"scope\": \"test-scope\", \"resourceURI\": \"https://example.com/resource\", \"authorizationURI\": \"https://example.com/authorization\" }")
                                      .addHeader("Content-Type", "application/json"));

        Mono<ClientAccessTokenResponse> response = oAuthTokenClient.clientAccessToken(request);

        StepVerifier.create(response)
                    .expectNextMatches(clientAccessTokenResponse -> "client-test-token".equals(clientAccessTokenResponse.getAccessToken()) &&
                                                                    "Bearer".equals(clientAccessTokenResponse.getTokenType()) &&
                                                                    3600 == clientAccessTokenResponse.getExpiresIn() &&
                                                                    "test-scope".equals(clientAccessTokenResponse.getScope()) &&
                                                                    "https://example.com/resource".equals(
                                                                            clientAccessTokenResponse.getResourceUri()) &&
                                                                    "https://example.com/authorization".equals(
                                                                            clientAccessTokenResponse.getAuthorizationUri()))
                    .verifyComplete();
    }
}
