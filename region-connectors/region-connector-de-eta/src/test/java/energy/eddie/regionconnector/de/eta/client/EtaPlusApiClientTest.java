package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.AuthenticationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.DeserializationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusServerException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusOperationExceptions.RateLimitException;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EtaPlusApiClientTest {

    private static MockWebServer server;
    private static JsonMapper jsonMapper;
    private static WebClient webClient;
    private static EtaPlusApiClient apiClient;
    private static DeEtaPlusConfiguration config;

    @BeforeAll
    static void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        jsonMapper = new JsonMapper();
        String baseUrl = "http://localhost:" + server.getPort();
        webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs()
                        .jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper, MediaType.APPLICATION_JSON)))
                .build();
        config = new DeEtaPlusConfiguration(
                "eligible-party",
                baseUrl,
                "client-id",
                "client-secret",
                "/meters/historical",
                "/v1/permissions/{id}",
                30,
                3, 0,
                true,
                false,
                null,
                null
        );
        apiClient = new EtaPlusApiClient(webClient, config);
    }

    @AfterAll
    static void tearDown() throws IOException {
        server.shutdown();
    }


    /**
     * Drains all pending requests from the MockWebServer queue and returns the last one.
     * This ensures we get the request from the current test, not a stale one from a previous test.
     */
    private RecordedRequest takeLatestRequest() throws InterruptedException {
        RecordedRequest latest = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest next;
        while ((next = server.takeRequest(100, TimeUnit.MILLISECONDS)) != null) {
            latest = next;
        }
        return latest;
    }

    private DePermissionRequest buildRequest(String permissionId, LocalDate start, LocalDate end) {
        return new DePermissionRequestBuilder()
                .permissionId(permissionId)
                .meteringPointId("malo-test")
                .start(start)
                .end(end)
                .build();
    }

    @Test
    void fetchMeteredData_validReadings_mappedToEtaPlusMeteredData() {
        String responseBody = """
                [
                  {
                    "timestamp": "2024-06-01T12:00:00Z",
                    "value": 42.5,
                    "unit": "kWh",
                    "status": "VALIDATED"
                  }
                ]
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        DePermissionRequest request = buildRequest(
                "perm-1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .assertNext(data -> {
                    assertThat(data.meteringPointId()).isEqualTo("malo-test");
                    assertThat(data.startDate()).isEqualTo(LocalDate.of(2024, 1, 1));
                    assertThat(data.readings()).hasSize(1);
                    EtaPlusMeteredData.MeterReading reading = data.readings().get(0);
                    assertThat(reading.value()).isEqualTo(42.5);
                    assertThat(reading.unit()).isEqualTo("kWh");
                    assertThat(reading.quality()).isEqualTo("VALIDATED");
                })
                .verifyComplete();
    }

    @Test
    void fetchMeteredData_pastEndDate_usedUnchanged() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        LocalDate pastEnd = LocalDate.of(2024, 6, 30);
        DePermissionRequest request = buildRequest(
                "perm-past", LocalDate.of(2024, 1, 1), pastEnd);

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .assertNext(data -> assertThat(data.endDate()).isEqualTo(pastEnd))
                .verifyComplete();
    }

    @Test
    void fetchMeteredData_futureEndDate_cappedToToday() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        LocalDate futureEnd = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).plusYears(1);
        DePermissionRequest request = buildRequest(
                "perm-future", LocalDate.of(2024, 1, 1), futureEnd);

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .assertNext(data -> {
                    LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
                    assertThat(data.endDate()).isEqualTo(today);
                })
                .verifyComplete();
    }

    @Test
    void fetchMeteredData_emptyResponse_returnsEmptyReadingsList() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        DePermissionRequest request = buildRequest(
                "perm-empty", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .assertNext(data -> assertThat(data.readings()).isEmpty())
                .verifyComplete();
    }

    @Test
    void fetchMeteredData_serverReturns422_errorPropagatedAsWebClientResponseException() {
        server.enqueue(new MockResponse().setResponseCode(422));

        DePermissionRequest request = buildRequest(
                "perm-error", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void fetchMeteredData_serverReturns401_throwsAuthenticationException() {
        server.enqueue(new MockResponse().setResponseCode(401));

        DePermissionRequest request = buildRequest(
                "perm-auth", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(AuthenticationException.class);
                    assertThat(error.getMessage()).contains("perm-auth");
                    assertThat(((AuthenticationException) error).statusCode()).isEqualTo(401);
                })
                .verify();
    }

    @Test
    void fetchMeteredData_serverReturns429_retriesAndThrowsRateLimitException() {
        // 1 initial + 3 retries = 4 total attempts
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(429));
        }

        DePermissionRequest request = buildRequest(
                "perm-rate", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RateLimitException.class);
                    assertThat(error.getMessage()).contains("perm-rate");
                })
                .verify();

        assertThat(server.getRequestCount()).isGreaterThan(1);
    }

    @Test
    void fetchMeteredData_serverReturns429ThenRecovers_returnsData() {
        server.enqueue(new MockResponse().setResponseCode(429));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        DePermissionRequest request = buildRequest(
                "perm-rate-recover", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .assertNext(data -> assertThat(data.readings()).isEmpty())
                .verifyComplete();
    }

    @Test
    void fetchMeteredData_serverReturns500_retriesAndThrowsEtaPlusServerException() {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(500));
        }

        DePermissionRequest request = buildRequest(
                "perm-5xx", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(EtaPlusServerException.class);
                    assertThat(error.getMessage()).contains("perm-5xx");
                    assertThat(((EtaPlusServerException) error).statusCode()).isEqualTo(500);
                })
                .verify();
    }

    @Test
    void fetchMeteredData_serverReturns503_retriesAndThrowsEtaPlusServerException() {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(503));
        }

        DePermissionRequest request = buildRequest(
                "perm-503", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(EtaPlusServerException.class);
                    assertThat(((EtaPlusServerException) error).statusCode()).isEqualTo(503);
                })
                .verify();
    }

    @Test
    void fetchMeteredData_malformedResponse_throwsDeserializationException() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("this is not valid json"));

        DePermissionRequest request = buildRequest(
                "perm-malformed", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DeserializationException.class);
                    assertThat(error.getMessage()).contains("perm-malformed");
                })
                .verify();
    }

    @Test
    void fetchMeteredData_sendsCorrectQueryParameters() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 6, 30);
        DePermissionRequest request = new DePermissionRequestBuilder()
                .permissionId("perm-params")
                .meteringPointId("malo-xyz")
                .start(start)
                .end(end)
                .build();

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .assertNext(data -> {})
                .verifyComplete();

        RecordedRequest recorded = takeLatestRequest();
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).contains("meteringPointId=malo-xyz");
        assertThat(recorded.getPath()).contains("from=2024-01-01T00:00");
        assertThat(recorded.getPath()).contains("to=2024-06-30T00:00");
    }

    @Test
    void fetchMeteredData_sendsBearerTokenInAuthorizationHeader() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        DePermissionRequest request = buildRequest(
                "perm-bearer", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "my-access-token"))
                .assertNext(data -> {})
                .verifyComplete();

        RecordedRequest recorded = takeLatestRequest();
        assertThat(recorded).isNotNull();
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer my-access-token");
    }

    @Test
    void fetchMeteredData_sendsRequestToCorrectEndpoint() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        DePermissionRequest request = buildRequest(
                "perm-path", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token"))
                .assertNext(data -> {})
                .verifyComplete();

        RecordedRequest recorded = takeLatestRequest();
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).startsWith("/meters/historical");
    }

    // ---- checkPermissionValidity ----

    @Test
    void checkPermissionValidity_200Response_returnsTrue() {
        server.enqueue(new MockResponse().setResponseCode(200));

        DePermissionRequest request = buildRequest(
                "perm-valid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        StepVerifier.create(apiClient.checkPermissionValidity(request))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void checkPermissionValidity_403Response_returnsFalse() {
        server.enqueue(new MockResponse().setResponseCode(403));

        DePermissionRequest request = buildRequest(
                "perm-forbidden", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        StepVerifier.create(apiClient.checkPermissionValidity(request))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void checkPermissionValidity_500Response_retriesAndReturnsFalse() {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(500));
        }

        DePermissionRequest request = buildRequest(
                "perm-server-error", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        StepVerifier.create(apiClient.checkPermissionValidity(request))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void checkPermissionValidity_401Response_throwsAuthenticationException() {
        server.enqueue(new MockResponse().setResponseCode(401));

        DePermissionRequest request = buildRequest(
                "perm-auth-check", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        StepVerifier.create(apiClient.checkPermissionValidity(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(AuthenticationException.class);
                    assertThat(error.getMessage()).contains("perm-auth-check");
                    assertThat(((AuthenticationException) error).statusCode()).isEqualTo(401);
                })
                .verify();
    }

    @Test
    void checkPermissionValidity_429Response_retriesAndThrowsRateLimitException() {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(429));
        }

        DePermissionRequest request = buildRequest(
                "perm-rate-check", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        StepVerifier.create(apiClient.checkPermissionValidity(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RateLimitException.class);
                    assertThat(error.getMessage()).contains("perm-rate-check");
                })
                .verify();
    }

    @Test
    void checkPermissionValidity_usesHeadMethodAndCorrectPathWithPermissionId() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        DePermissionRequest request = buildRequest(
                "perm-abc-123", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        StepVerifier.create(apiClient.checkPermissionValidity(request))
                .expectNext(true)
                .verifyComplete();

        RecordedRequest recorded = takeLatestRequest();
        assertThat(recorded).isNotNull();
        assertThat(recorded.getMethod()).isEqualTo("HEAD");
        assertThat(recorded.getPath()).isEqualTo("/v1/permissions/perm-abc-123");
    }
}
