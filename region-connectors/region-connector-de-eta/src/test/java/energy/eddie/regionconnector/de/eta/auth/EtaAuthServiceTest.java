package energy.eddie.regionconnector.de.eta.auth;

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

class EtaAuthServiceTest {

    private MockWebServer mockWebServer;
    private EtaAuthService service;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String tokenUrl = mockWebServer.url("/token").toString();
        DeEtaPlusConfiguration.AuthConfig authConfig = new DeEtaPlusConfiguration.AuthConfig(
                "client", "secret", tokenUrl, "http://auth.url", "http://redirect.uri", "scope");

        DeEtaPlusConfiguration configuration = new DeEtaPlusConfiguration(
                "partner", "http://api.url", authConfig, null);

        service = new EtaAuthService(configuration);
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
                        .setHeader("Content-Type", "application/json;charset=UTF-8")
                        .setBody(
                                "{\"access_token\": \"acc-token\", \"token_type\": \"bearer\", \"refresh_token\": \"ref-token\", \"expires_in\": 3600}"));

        Mono<AuthTokenResponse> resultMono = service.exchangeCodeForToken("auth-code", "http://redirect.uri");

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

        Mono<AuthTokenResponse> resultMono = service.exchangeCodeForToken("auth-code", "client-id");

        StepVerifier.create(resultMono)
                .assertNext(res -> {
                    assertThat(res.success()).isFalse();
                    assertThat(res.getAccessToken()).isNull();
                }).verifyComplete();
    }
}
