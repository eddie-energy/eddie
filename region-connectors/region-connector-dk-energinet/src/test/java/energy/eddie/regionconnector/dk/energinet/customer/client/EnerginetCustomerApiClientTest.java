package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.stream.Stream;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnerginetCustomerApiClientTest {
    private static final int MAX_PERIOD = 730;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockWebServer mockServer;

    @BeforeEach
    void setup() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    static Stream<Arguments> getTimeSeries_invalidTimeFrame_throws() {
        var endBeforeStart = LocalDate.of(2023, 1, 1);
        var tomorrow = LocalDate.now(DK_ZONE_ID).plusDays(1);
        var start = LocalDate.of(2023, 2, 1);
        return Stream.of(
                Arguments.of(start, endBeforeStart),
                Arguments.of(start, tomorrow),
                Arguments.of(tomorrow, tomorrow),
                Arguments.of(start, start)
        );
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @ParameterizedTest
    @MethodSource
    void getTimeSeries_invalidTimeFrame_throws(LocalDate start, LocalDate end) {
        // Given
        var config = new EnerginetConfiguration(mockServer.url("/").toString());
        var client = new EnerginetCustomerApiClient(config, WebClient.builder());
        String token = "token";
        var meteringPointsRequest = new MeteringPointsRequest();

        // When
        // Then
        assertThrows(DateTimeException.class,
                     () -> client.getTimeSeries(start,
                                                end,
                                                Granularity.P1D,
                                                meteringPointsRequest,
                                                token
                     ));
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Test
    void getTimeSeries_invalidTimeFrame_exceedMaxPeriod_throws() {
        // Given
        var end = LocalDate.now(DK_ZONE_ID).minusDays(1);
        var startExceedsMaxPeriod = end.minusDays(MAX_PERIOD + 1);
        var config = new EnerginetConfiguration("path");
        var client = new EnerginetCustomerApiClient(config, WebClient.builder());
        var meteringPointsRequest = new MeteringPointsRequest();

        // When
        // Then
        assertThrows(DateTimeException.class,
                     () -> client.getTimeSeries(startExceedsMaxPeriod,
                                                end,
                                                Granularity.P1D,
                                                meteringPointsRequest,
                                                "token"
                     ));
    }

    @ParameterizedTest
    @MethodSource("isAlive_returnsMonoWithBoolean")
    void isAlive_returnsMonoWithBoolean(boolean expected) {
        // Given
        var config = new EnerginetConfiguration(mockServer.url("/").toString());
        mockServer.enqueue(new MockResponse()
                                   .setResponseCode(200)
                                   .setHeader("Content-Type", "application/json")
                                   .setBody(Boolean.toString(expected)));
        var customerApi = new EnerginetCustomerApiClient(config, WebClient.builder());

        // When
        Mono<Boolean> result = customerApi.isAlive();

        // Then
        StepVerifier.create(result)
                    .expectNext(expected)
                    .verifyComplete();
    }

    @Test
    void accessToken_returnsToken() {
        // Given
        var body = new StringApiResponse().result("token");
        var config = new EnerginetConfiguration(mockServer.url("/").toString());
        mockServer.enqueue(new MockResponse()
                                   .setHeader("Content-Type", "application/json")
                                   .setResponseCode(200)
                                   .setBody(objectMapper.writeValueAsString(body)));
        var customerApi = new EnerginetCustomerApiClient(config, WebClient.builder());

        // When
        Mono<String> result = customerApi.accessToken("refreshToken");

        // Then
        StepVerifier.create(result)
                    .expectNext("token")
                    .verifyComplete();
    }

    @Test
    void accessToken_throwsIfUnauthorized() {
        // Given
        mockServer.enqueue(new MockResponse()
                                   .setHeader("Content-Type", "application/json")
                                   .setResponseCode(HttpStatus.UNAUTHORIZED.value()));
        var config = new EnerginetConfiguration(mockServer.url("/").toString());
        var customerApi = new EnerginetCustomerApiClient(config, WebClient.builder());

        // When
        Mono<String> result = customerApi.accessToken("refreshToken");

        // Then
        StepVerifier.create(result)
                    .expectError(WebClientResponseException.Unauthorized.class)
                    .verify();
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void getTimeSeries_returnsApiResponse() {
        // Given
        var start = LocalDate.of(2023, 1, 1);
        var end = LocalDate.of(2023, 1, 2);
        var document = new MyEnergyDataMarketDocument()
                .periodTimeInterval(
                        new PeriodtimeInterval()
                                .start(start.toString())
                                .end(end.toString())
                )
                .addTimeSeriesItem(
                        new TimeSeries()
                                .addPeriodItem(
                                        new Period()
                                                .resolution("PT15M")
                                                .addPointItem(
                                                        new Point()
                                                                .outQuantityQuality("A04")
                                                                .outQuantityQuantity(Double.toString(0.0))
                                                )
                                )
                );
        var data = new MyEnergyDataMarketDocumentResponseListApiResponse()
                .addResultItem(
                        new MyEnergyDataMarketDocumentResponse()
                                .id("ID")
                                .myEnergyDataMarketDocument(document)
                );
        mockServer.enqueue(new MockResponse()
                                   .setResponseCode(200)
                                   .setHeader("Content-Type", "application/json")
                                   .setBody(objectMapper.writeValueAsString(data)));
        var config = new EnerginetConfiguration(mockServer.url("/").toString());
        var customerApi = new EnerginetCustomerApiClient(config, WebClient.builder());

        // When
        var result = customerApi.getTimeSeries(
                start,
                end,
                PT15M,
                new MeteringPointsRequest(),
                "accessToken"
        );

        // Then
        StepVerifier.create(result)
                    .assertNext(response -> assertAll(
                            () -> assertNotNull(response.getResult()),
                            () -> assertNotNull(response.getResult().getFirst()),
                            () -> assertEquals("ID", response.getResult().getFirst().getId())
                    ))
                    .verifyComplete();
    }

    @Test
    void getTimeSeries_throwsExceptionWhenUnauthorized() {
        // Given
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 2);
        mockServer.enqueue(new MockResponse()
                                   .setHeader("Content-Type", "application/json")
                                   .setResponseCode(HttpStatus.UNAUTHORIZED.value()));
        var config = new EnerginetConfiguration(mockServer.url("/").toString());
        var customerApi = new EnerginetCustomerApiClient(config, WebClient.builder());

        // When
        var result = customerApi.getTimeSeries(
                start,
                end,
                PT15M,
                new MeteringPointsRequest(),
                "accessToken"
        );

        // Then
        StepVerifier.create(result)
                    .expectError(WebClientResponseException.Unauthorized.class)
                    .verify();
    }

    @Test
    void getMeteringPointDetails_returnsMeteringPointDetails() {
        // Given
        var document = new MeteringPointDetailsCustomerDtoResponseListApiResponse();
        mockServer.enqueue(new MockResponse()
                                   .setResponseCode(200)
                                   .setHeader("Content-Type", "application/json")
                                   .setBody(objectMapper.writeValueAsString(document)));
        var config = new EnerginetConfiguration(mockServer.url("/").toString());

        var customerApi = new EnerginetCustomerApiClient(config, WebClient.builder());

        // When
        var result = customerApi.getMeteringPointDetails(
                new MeteringPointsRequest(), "accessToken"
        );

        // Then
        StepVerifier.create(result)
                    .assertNext(response -> assertEquals(document, response))
                    .verifyComplete();
    }

    @Test
    void getMeteringPointDetails_throwsExceptionWhenUnauthorized() {
        // Given
        mockServer.enqueue(new MockResponse()
                                   .setHeader("Content-Type", "application/json")
                                   .setResponseCode(HttpStatus.UNAUTHORIZED.value()));
        var config = new EnerginetConfiguration(mockServer.url("/").toString());
        var customerApi = new EnerginetCustomerApiClient(config, WebClient.builder());

        // When
        var result = customerApi.getMeteringPointDetails(
                new MeteringPointsRequest(),
                "accessToken"
        );

        // Then
        StepVerifier.create(result)
                    .expectError(WebClientResponseException.Unauthorized.class)
                    .verify();
    }

    private static Stream<Arguments> isAlive_returnsMonoWithBoolean() {
        return Stream.of(
                Arguments.of(true),
                Arguments.of(false)
        );
    }
}
