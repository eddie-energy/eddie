package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.IsAliveApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.MeterDataApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.MeteringPointsApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.TokenApi;
import energy.eddie.regionconnector.dk.energinet.customer.invoker.ApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class EnerginetCustomerApiClientTest {
    private static final int MAX_PERIOD = 730;
    @Mock
    private IsAliveApi isAliveApi;
    @Mock
    private TokenApi tokenApi;
    @Mock
    private MeterDataApi meterDataApi;
    @Mock
    private MeteringPointsApi meteringPointsApi;
    @Mock
    private EnerginetConfiguration config;
    @Mock
    private MeteringPointsRequest meteringPointsRequest;
    @Mock
    private Granularity granularity;

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

    static Stream<Arguments> isAlive_returnsMonoWithBoolean() {
        return Stream.of(
                Arguments.of(true),
                Arguments.of(false)
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void health_returnHealthUpState() {
        // Given
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config, WebClient.create());
        var spy = spy(client);
        doReturn(Mono.just(true)).when(spy).isAlive();

        // When
        Map<String, HealthState> actualHealth = spy.health().block();

        // Then
        assertAll(
                () -> assertNotNull(actualHealth),
                () -> assertTrue(actualHealth.containsKey("isAliveApi")),
                () -> assertEquals(HealthState.UP, actualHealth.get("isAliveApi"))
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void health_returnHealthDownState() {
        // Given
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config, WebClient.create());
        var spy = spy(client);
        doReturn(Mono.just(false)).when(spy).isAlive();

        // When
        Map<String, HealthState> actualHealth = spy.health().block();

        // Then
        assertAll(
                () -> assertNotNull(actualHealth),
                () -> assertTrue(actualHealth.containsKey("isAliveApi")),
                () -> assertEquals(HealthState.DOWN, actualHealth.get("isAliveApi"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void getTimeSeries_invalidTimeFrame_throws(LocalDate start, LocalDate end) {
        // Given
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config, WebClient.create());
        String token = "token";
        UUID userCorrelationId = UUID.randomUUID();

        // When
        // Then
        //noinspection ReactiveStreamsUnusedPublisher
        assertThrows(DateTimeException.class,
                     () -> client.getTimeSeries(start,
                                                end,
                                                granularity,
                                                meteringPointsRequest,
                                                token,
                                                userCorrelationId));
    }

    @Test
    void getTimeSeries_invalidTimeFrame_exceedMaxPeriod_throws() {
        // Given
        var end = LocalDate.now(DK_ZONE_ID).minusDays(1);
        var startExceedsMaxPeriod = end.minusDays(MAX_PERIOD + 1);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config, WebClient.create());
        UUID userCorrelationId = UUID.randomUUID();


        // When
        // Then
        //noinspection ReactiveStreamsUnusedPublisher
        assertThrows(DateTimeException.class,
                     () -> client.getTimeSeries(startExceedsMaxPeriod,
                                                end,
                                                granularity,
                                                meteringPointsRequest,
                                                "token",
                                                userCorrelationId));
    }

    @ParameterizedTest
    @MethodSource
    void isAlive_returnsMonoWithBoolean(boolean expected) {
        // Given
        when(isAliveApi.apiIsaliveGet()).thenReturn(Mono.just(expected));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), null, null, isAliveApi, null);

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
        when(tokenApi.apiTokenGet()).thenReturn(Mono.just(new StringApiResponse().result("token")));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), tokenApi, null, null, null);

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
        HttpClientErrorException unauthorized = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED,
                                                                                "Unauthorized",
                                                                                HttpHeaders.EMPTY,
                                                                                "".getBytes(StandardCharsets.UTF_8),
                                                                                StandardCharsets.UTF_8);
        when(tokenApi.apiTokenGet()).thenReturn(Mono.error(unauthorized));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), tokenApi, null, null, null);

        // When
        Mono<String> result = customerApi.accessToken("refreshToken");

        // Then
        StepVerifier.create(result)
                    .expectError(HttpClientErrorException.Unauthorized.class)
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
        when(meterDataApi.apiMeterdataGettimeseriesDateFromDateToAggregationPost(anyString(),
                                                                                 anyString(),
                                                                                 anyString(),
                                                                                 any(),
                                                                                 any()))
                .thenReturn(Mono.just(data));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), null, meterDataApi, null, null);

        // When
        var result = customerApi.getTimeSeries(
                start,
                end,
                PT15M,
                new MeteringPointsRequest(),
                "accessToken",
                UUID.randomUUID()
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
        HttpClientErrorException unauthorized = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED,
                                                                                "Unauthorized",
                                                                                HttpHeaders.EMPTY,
                                                                                "".getBytes(StandardCharsets.UTF_8),
                                                                                StandardCharsets.UTF_8);
        when(meterDataApi.apiMeterdataGettimeseriesDateFromDateToAggregationPost(anyString(),
                                                                                 anyString(),
                                                                                 anyString(),
                                                                                 any(),
                                                                                 any()))
                .thenReturn(Mono.error(unauthorized));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), null, meterDataApi, null, null);

        // When
        var result = customerApi.getTimeSeries(
                start,
                end,
                PT15M,
                new MeteringPointsRequest(),
                "accessToken",
                UUID.randomUUID()
        );

        // Then
        StepVerifier.create(result)
                    .expectError(HttpClientErrorException.Unauthorized.class)
                    .verify();
    }

    @Test
    void getMeteringPointDetails_returnsMeteringPointDetails() {
        // Given
        var document = new MeteringPointDetailsCustomerDtoResponseListApiResponse();

        when(meteringPointsApi.apiMeteringpointsMeteringpointGetdetailsPost(any()))
                .thenReturn(Mono.just(document));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), null, null, null, meteringPointsApi);

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
        HttpClientErrorException unauthorized = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED,
                                                                                "Unauthorized",
                                                                                HttpHeaders.EMPTY,
                                                                                "".getBytes(StandardCharsets.UTF_8),
                                                                                StandardCharsets.UTF_8);
        when(meteringPointsApi.apiMeteringpointsMeteringpointGetdetailsPost(any()))
                .thenReturn(Mono.error(unauthorized));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), null, null, null, meteringPointsApi);

        // When
        var result = customerApi.getMeteringPointDetails(
                new MeteringPointsRequest(),
                "accessToken"
        );

        // Then
        StepVerifier.create(result)
                    .expectError(HttpClientErrorException.Unauthorized.class)
                    .verify();
    }
}
