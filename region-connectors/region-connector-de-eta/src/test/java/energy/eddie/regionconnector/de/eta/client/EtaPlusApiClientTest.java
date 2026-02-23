package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class EtaPlusApiClientTest {

    private static MockWebServer server;
    private static JsonMapper jsonMapper;
    private static WebClient webClient;
    private static EtaPlusApiClient apiClient;
    private static DeEtaPlusConfiguration config;

    @Mock
    private ObjectMapper mockObjectMapper;

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
                "/api/v1/metered-data",
                "/api/v1/permissions/{id}"
        );
        apiClient = new EtaPlusApiClient(webClient, jsonMapper, config);
    }

    @AfterAll
    static void tearDown() throws IOException {
        server.shutdown();
    }

    @AfterEach
    void drainRequestQueue() throws InterruptedException {
        while (server.takeRequest(100, TimeUnit.MILLISECONDS) != null) {
        }
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

        StepVerifier.create(apiClient.fetchMeteredData(request))
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

        StepVerifier.create(apiClient.fetchMeteredData(request))
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

        StepVerifier.create(apiClient.fetchMeteredData(request))
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

        StepVerifier.create(apiClient.fetchMeteredData(request))
                .assertNext(data -> assertThat(data.readings()).isEmpty())
                .verifyComplete();
    }

    @Test
    void fetchMeteredData_rawJsonContainsSerializedReadings() {
        String responseBody = "[{\"timestamp\":\"2024-06-01T12:00:00Z\",\"value\":1.0,\"unit\":\"kWh\",\"status\":\"VALID\"}]";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        DePermissionRequest request = buildRequest(
                "perm-raw", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request))
                .assertNext(data -> {
                    assertThat(data.rawJson()).isNotNull();
                    assertThat(data.rawJson()).isNotEmpty();
                    assertThat(data.rawJson()).isNotEqualTo("[]");
                })
                .verifyComplete();
    }

    @Test
    void fetchMeteredData_serializationFailure_rawJsonFallsBackToEmptyArray() {
        String responseBody = "[{\"timestamp\":\"2024-06-01T12:00:00Z\",\"value\":1.0,\"unit\":\"kWh\",\"status\":\"VALID\"}]";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        JacksonException jacksonException = mock(JacksonException.class);
        doThrow(jacksonException).when(mockObjectMapper).writeValueAsString(any());

        EtaPlusApiClient failingSerializerClient = new EtaPlusApiClient(webClient, mockObjectMapper, config);
        DePermissionRequest request = buildRequest(
                "perm-fail", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(failingSerializerClient.fetchMeteredData(request))
                .assertNext(data -> assertThat(data.rawJson()).isEqualTo("[]"))
                .verifyComplete();
    }

    @Test
    void fetchMeteredData_serverReturns4xx_errorPropagated() {
        server.enqueue(new MockResponse().setResponseCode(422));

        DePermissionRequest request = buildRequest(
                "perm-error", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request))
                .expectError(WebClientResponseException.class)
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

        StepVerifier.create(apiClient.fetchMeteredData(request))
                .assertNext(data -> {})
                .verifyComplete();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).contains("meteringPointId=malo-xyz");
        assertThat(recorded.getPath()).contains("start=2024-01-01");
        assertThat(recorded.getPath()).contains("end=2024-06-30");
    }

    @Test
    void fetchMeteredData_sendsRequestToCorrectEndpoint() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        DePermissionRequest request = buildRequest(
                "perm-path", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        StepVerifier.create(apiClient.fetchMeteredData(request))
                .assertNext(data -> {})
                .verifyComplete();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).startsWith("/api/v1/metered-data");
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
    void checkPermissionValidity_500Response_returnsFalse() {
        server.enqueue(new MockResponse().setResponseCode(500));

        DePermissionRequest request = buildRequest(
                "perm-server-error", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        StepVerifier.create(apiClient.checkPermissionValidity(request))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void checkPermissionValidity_usesHeadMethodAndCorrectPathWithPermissionId() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        DePermissionRequest request = buildRequest(
                "perm-abc-123", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        StepVerifier.create(apiClient.checkPermissionValidity(request))
                .expectNext(true)
                .verifyComplete();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("HEAD");
        assertThat(recorded.getPath()).isEqualTo("/api/v1/permissions/perm-abc-123");
    }
}
