package energy.eddie.regionconnector.de.eta.oauth;

import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class EtaOAuthServiceTest {

    private MockWebServer mockWebServer;
    private EtaOAuthService service;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String tokenUrl = mockWebServer.url("/token").toString();
        DeEtaPlusConfiguration.OAuthConfig oauthConfig = new DeEtaPlusConfiguration.OAuthConfig(
                "client", "secret", tokenUrl, "http://auth.url", "http://redirect.uri", "scope"
        );

        DeEtaPlusConfiguration configuration = new DeEtaPlusConfiguration(
                "partner", "http://api.url", oauthConfig, null
        );

        service = new EtaOAuthService(configuration);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void exchangeCodeForTokenShouldCallWebClientProperly() {
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                                "{\"success\": true, \"data\": {\"token\": \"acc-token\", \"refreshToken\": \"ref-token\"}}"
                        )
        );

        Mono<OAuthTokenResponse> resultMono = service.exchangeCodeForToken("auth-code", "client-id");

        StepVerifier.create(resultMono)
                    .assertNext(res -> {
                        assertThat(res.success()).isTrue();
                        assertThat(res.getAccessToken()).isEqualTo("acc-token");
                        assertThat(res.getRefreshToken()).isEqualTo("ref-token");
                    })
                    .verifyComplete();
    }

    @Test
    void exchangeCodeForTokenWhenUnsuccessfulResponseShouldNotFailInProcessing() {
        mockWebServer.enqueue(new MockResponse()
                                      .setResponseCode(400)
                                      .setHeader("Content-Type", "application/json")
                                      .setBody("{\"error\": \"invalid_request\"}"));

        Mono<OAuthTokenResponse> resultMono = service.exchangeCodeForToken("auth-code", "client-id");

        StepVerifier.create(resultMono)
                    .assertNext(res -> {
                        assertThat(res.success()).isFalse();
                        assertThat(res.getAccessToken()).isNull();
                    }).verifyComplete();
    }
}
