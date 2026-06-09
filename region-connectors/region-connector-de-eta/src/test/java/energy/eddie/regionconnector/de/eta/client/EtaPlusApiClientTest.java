package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.AuthenticationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.DeserializationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusBadRequestException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusForbiddenException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusNotFoundException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusServerException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusOperationExceptions.RateLimitException;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusAccountingPointData;
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
                "/meters/accounting-point",
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
                    "status": "VALIDATED",
                    "direction": "Consumption"
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
                    assertThat(reading.direction()).isEqualTo("Consumption");
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
    void fetchMeteredData_explicitWindow_usesGivenRange_notPermissionRange() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        // Permission spans Jan–Jun; retransmission asks only for February.
        DePermissionRequest request = buildRequest(
                "perm-window", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));
        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 2, 29);

        StepVerifier.create(apiClient.fetchMeteredData(request, "test-token", from, to))
                .assertNext(data -> {
                    assertThat(data.startDate()).isEqualTo(from);
                    assertThat(data.endDate()).isEqualTo(to);
                })
                .verifyComplete();

        RecordedRequest recorded = takeLatestRequest();
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).contains("from=2024-02-01T00:00");
        assertThat(recorded.getPath()).contains("to=2024-02-29T00:00");
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

    // ------------------------------------------------------------------
    // fetchAccountingPointData
    // ------------------------------------------------------------------

    private DePermissionRequest buildApRequest(String permissionId, String meteringPointId) {
        return new DePermissionRequestBuilder()
                .permissionId(permissionId)
                .meteringPointId(meteringPointId)
                .build();
    }

    @Test
    void fetchAccountingPointData_validResponse_mapsToDto() {
        String body = """
                {
                  "meteringPointId": "malo-1",
                  "customerId": "42",
                  "energyType": "ELECTRICITY",
                  "direction": "Consumption",
                  "deliveryAddress": {
                    "streetName": "Hauptstraße",
                    "city": "Berlin",
                    "country": "DE"
                  },
                  "contractParty": {
                    "firstName": "Max",
                    "surName": "Mustermann"
                  }
                }
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body));

        DePermissionRequest request = buildApRequest("perm-ap-1", "malo-1");

        StepVerifier.create(apiClient.fetchAccountingPointData(request, "test-token"))
                .assertNext(data -> {
                    assertThat(data.meteringPointId()).isEqualTo("malo-1");
                    assertThat(data.customerId()).isEqualTo("42");
                    assertThat(data.deliveryAddress().city()).isEqualTo("Berlin");
                    assertThat(data.contractParty().surName()).isEqualTo("Mustermann");
                })
                .verifyComplete();
    }

    @Test
    void fetchAccountingPointData_sendsQueryParamAndBearer() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"meteringPointId\":\"malo-q\"}"));

        DePermissionRequest request = buildApRequest("perm-ap-q", "malo-q");

        StepVerifier.create(apiClient.fetchAccountingPointData(request, "ap-bearer"))
                .assertNext(data -> assertThat(data.meteringPointId()).isEqualTo("malo-q"))
                .verifyComplete();

        RecordedRequest recorded = takeLatestRequest();
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).startsWith("/meters/accounting-point?meteringPointId=malo-q");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer ap-bearer");
    }

    @Test
    void fetchAccountingPointData_400_throwsBadRequestException_noRetry() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"Success\":false,\"Message\":\"meteringPointId is required.\"}"));

        DePermissionRequest request = buildApRequest("perm-ap-400", "malo-400");

        StepVerifier.create(apiClient.fetchAccountingPointData(request, "token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(EtaPlusBadRequestException.class);
                    assertThat(((EtaPlusBadRequestException) error).statusCode()).isEqualTo(400);
                    assertThat(error.getMessage()).contains("perm-ap-400");
                })
                .verify();
    }

    @Test
    void fetchAccountingPointData_401_throwsAuthenticationException_noRetry() {
        server.enqueue(new MockResponse().setResponseCode(401));

        DePermissionRequest request = buildApRequest("perm-ap-401", "malo-401");

        StepVerifier.create(apiClient.fetchAccountingPointData(request, "token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(AuthenticationException.class);
                    assertThat(((AuthenticationException) error).statusCode()).isEqualTo(401);
                    assertThat(error.getMessage()).contains("perm-ap-401");
                })
                .verify();
    }

    @Test
    void fetchAccountingPointData_403_throwsForbiddenException_noRetry() {
        server.enqueue(new MockResponse().setResponseCode(403));

        DePermissionRequest request = buildApRequest("perm-ap-403", "malo-403");

        StepVerifier.create(apiClient.fetchAccountingPointData(request, "token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(EtaPlusForbiddenException.class);
                    assertThat(((EtaPlusForbiddenException) error).statusCode()).isEqualTo(403);
                })
                .verify();
    }

    @Test
    void fetchAccountingPointData_404_throwsNotFoundException_noRetry() {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"Success\":false,\"Message\":\"Device malo-404 not found.\"}"));

        DePermissionRequest request = buildApRequest("perm-ap-404", "malo-404");

        StepVerifier.create(apiClient.fetchAccountingPointData(request, "token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(EtaPlusNotFoundException.class);
                    assertThat(((EtaPlusNotFoundException) error).statusCode()).isEqualTo(404);
                })
                .verify();
    }

    @Test
    void fetchAccountingPointData_429_retriesAndThrowsRateLimitException() {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(429));
        }
        DePermissionRequest request = buildApRequest("perm-ap-429", "malo-429");

        StepVerifier.create(apiClient.fetchAccountingPointData(request, "token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RateLimitException.class);
                    assertThat(error.getMessage()).contains("perm-ap-429");
                })
                .verify();

        assertThat(server.getRequestCount()).isGreaterThan(1);
    }

    @Test
    void fetchAccountingPointData_429ThenRecovers_returnsData() {
        server.enqueue(new MockResponse().setResponseCode(429));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"meteringPointId\":\"malo-recover\"}"));

        DePermissionRequest request = buildApRequest("perm-ap-recover", "malo-recover");

        StepVerifier.create(apiClient.fetchAccountingPointData(request, "token"))
                .assertNext(data -> assertThat(data.meteringPointId()).isEqualTo("malo-recover"))
                .verifyComplete();
    }

    @Test
    void fetchAccountingPointData_500_retriesAndThrowsServerException() {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(500));
        }
        DePermissionRequest request = buildApRequest("perm-ap-500", "malo-500");

        StepVerifier.create(apiClient.fetchAccountingPointData(request, "token"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(EtaPlusServerException.class);
                    assertThat(((EtaPlusServerException) error).statusCode()).isEqualTo(500);
                })
                .verify();
    }

    @Test
    void fetchAccountingPointData_malformedJson_throwsDeserializationException() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{not valid json"));

        DePermissionRequest request = buildApRequest("perm-ap-bad", "malo-bad");

        StepVerifier.create(apiClient.fetchAccountingPointData(request, "token"))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(DeserializationException.class))
                .verify();
    }

    @Test
    void fetchAccountingPointData_assertDtoTypeForCompiler() {
        EtaPlusAccountingPointData ignored = new EtaPlusAccountingPointData(
                "malo", null, null, null, null, null);
        assertThat(ignored.meteringPointId()).isEqualTo("malo");
    }
}
