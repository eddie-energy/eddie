package energy.eddie.regionconnector.de.eta.config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EtaPlusClientConfigTest {

    private static MockWebServer server;
    private static final EtaPlusClientConfig CONFIG = new EtaPlusClientConfig();

    @BeforeAll
    static void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        server.shutdown();
    }

    private static DeEtaPlusConfiguration configFor(String baseUrl) {
        return configFor(baseUrl, false, false);
    }

    private static DeEtaPlusConfiguration configFor(String baseUrl, boolean sslEnabled, boolean sslTrustAll) {
        return new DeEtaPlusConfiguration(
                "eligible-party",
                baseUrl,
                "my-client",
                "my-secret",
                "/meters/historical",
                "/v1/permissions/{id}",
                30,
                3, 2,
                sslEnabled,
                sslTrustAll,
                null,
                null
        );
    }

    @Test
    void etaWebClient_returnsNonNullWebClient() throws SSLException {
        String baseUrl = "http://localhost:" + server.getPort();

        WebClient webClient = CONFIG.etaWebClient(WebClient.builder(), configFor(baseUrl));

        assertThat(webClient).isNotNull();
    }

    @Test
    void etaWebClient_withHttpsUrlAndSslEnabled_doesNotThrow() {
        assertThatCode(() -> CONFIG.etaWebClient(
                WebClient.builder(),
                configFor("https://api.eta-plus.de", true, false)
        )).doesNotThrowAnyException();
    }

    @Test
    void etaWebClient_doesNotSetDefaultAuthorizationHeader() throws SSLException, InterruptedException {
        String baseUrl = "http://localhost:" + server.getPort();
        WebClient webClient = CONFIG.etaWebClient(WebClient.builder(), configFor(baseUrl));

        server.enqueue(new MockResponse().setResponseCode(200));
        webClient.get().uri("/test").retrieve().toBodilessEntity()
                .onErrorResume(e -> Mono.empty())
                .block();

        RecordedRequest request = server.takeRequest();
        String authHeader = request.getHeader("Authorization");
        assertThat(authHeader).isNull();
    }

    @Test
    void etaWebClient_usesBaseUrlFromConfiguration() throws SSLException, InterruptedException {
        String baseUrl = "http://localhost:" + server.getPort();
        WebClient webClient = CONFIG.etaWebClient(WebClient.builder(), configFor(baseUrl));

        server.enqueue(new MockResponse().setResponseCode(200));
        webClient.get().uri("/my-path").retrieve().toBodilessEntity()
                .onErrorResume(e -> Mono.empty())
                .block();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/my-path");
    }
}
