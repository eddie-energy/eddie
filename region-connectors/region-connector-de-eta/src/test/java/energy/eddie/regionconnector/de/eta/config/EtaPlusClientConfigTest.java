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
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        return new DeEtaPlusConfiguration(
                "eligible-party",
                baseUrl,
                "my-client",
                "my-secret",
                "/api/v1/metered-data",
                "/api/v1/permissions/{id}"
        );
    }

    @Test
    void etaWebClient_returnsNonNullWebClient() throws SSLException {
        String baseUrl = "http://localhost:" + server.getPort();

        WebClient webClient = CONFIG.etaWebClient(WebClient.builder(), configFor(baseUrl), false);

        assertThat(webClient).isNotNull();
    }

    @Test
    void etaWebClient_withHttpsUrlAndSslEnabled_doesNotThrow() {
        assertThatCode(() -> CONFIG.etaWebClient(
                WebClient.builder(),
                configFor("https://api.eta-plus.de"),
                true
        )).doesNotThrowAnyException();
    }

    @Test
    void etaWebClient_withHttpUrlAndSslEnabled_doesNotApplySsl() {
        // HTTP URL: the SSL branch requires URL to start with "https", so it is skipped
        assertThatCode(() -> CONFIG.etaWebClient(
                WebClient.builder(),
                configFor("http://api.eta-plus.de"),
                true
        )).doesNotThrowAnyException();
    }

    @Test
    void etaWebClient_withHttpsUrlAndSslDisabled_doesNotApplySsl() {
        // SSL flag is false → SSL branch is skipped even for HTTPS
        assertThatCode(() -> CONFIG.etaWebClient(
                WebClient.builder(),
                configFor("https://api.eta-plus.de"),
                false
        )).doesNotThrowAnyException();
    }

    @Test
    void etaWebClient_setsCorrectBasicAuthHeader() throws SSLException, InterruptedException {
        String baseUrl = "http://localhost:" + server.getPort();
        WebClient webClient = CONFIG.etaWebClient(WebClient.builder(), configFor(baseUrl), false);

        server.enqueue(new MockResponse().setResponseCode(200));
        webClient.get().uri("/test").retrieve().toBodilessEntity()
                .onErrorResume(e -> Mono.empty())
                .block();

        RecordedRequest request = server.takeRequest();
        String authHeader = request.getHeader("Authorization");
        String expectedCredentials = Base64.getEncoder()
                .encodeToString("my-client:my-secret".getBytes(StandardCharsets.UTF_8));
        assertThat(authHeader).isEqualTo("Basic " + expectedCredentials);
    }

    @Test
    void etaWebClient_usesBaseUrlFromConfiguration() throws SSLException, InterruptedException {
        String baseUrl = "http://localhost:" + server.getPort();
        WebClient webClient = CONFIG.etaWebClient(WebClient.builder(), configFor(baseUrl), false);

        server.enqueue(new MockResponse().setResponseCode(200));
        webClient.get().uri("/my-path").retrieve().toBodilessEntity()
                .onErrorResume(e -> Mono.empty())
                .block();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/my-path");
    }
}
