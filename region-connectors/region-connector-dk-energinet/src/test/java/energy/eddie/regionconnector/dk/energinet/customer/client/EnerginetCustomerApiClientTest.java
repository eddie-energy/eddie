package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.IsAliveApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.MeterDataApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.TokenApi;
import energy.eddie.regionconnector.dk.energinet.customer.invoker.ApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.model.Period;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnerginetCustomerApiClientTest {
    private static final ZoneId DK_ZONE_ID = EnerginetRegionConnector.DK_ZONE_ID;
    private static final int MAX_PERIOD = 730;

    static Stream<Arguments> getTimeSeries_invalidTimeFrame_throws() {
        var endBeforeStart = ZonedDateTime.of(LocalDate.of(2023, 1, 1).atStartOfDay(), DK_ZONE_ID);
        var today = ZonedDateTime.of(LocalDate.now(ZoneId.systemDefault()).atStartOfDay(), DK_ZONE_ID);
        var start = ZonedDateTime.of(LocalDate.of(2023, 2, 1).atStartOfDay(), DK_ZONE_ID);
        return Stream.of(
                Arguments.of(start, endBeforeStart),
                Arguments.of(start, today),
                Arguments.of(today, today),
                Arguments.of(start, start)
        );
    }

    static Stream<Arguments> isAlive_returnsMonoWithBoolean() {
        return Stream.of(
                Arguments.of(true),
                Arguments.of(false)
        );
    }

    @Test
    void health_returnHealthUpState() {
        // Given
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);
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

    @Test
    void health_returnHealthDownState() {
        // Given
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);
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
    void getTimeSeries_invalidTimeFrame_throws(ZonedDateTime start, ZonedDateTime end) {
        // Given
        Granularity granularity = mock(Granularity.class);
        var meteringPointsRequest = mock(MeteringPointsRequest.class);
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);
        String token = "token";
        UUID userCorrelationId = UUID.randomUUID();

        // When
        // Then
        //noinspection ReactiveStreamsUnusedPublisher
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(start, end, granularity, meteringPointsRequest, token, userCorrelationId));
    }

    @Test
    void getTimeSeries_invalidTimeFrame_exceedMaxPeriod_throws() {
        // Given
        var end = ZonedDateTime.of(LocalDate.now(ZoneId.systemDefault()).minusDays(1).atStartOfDay(), DK_ZONE_ID);
        var startExceedsMaxPeriod = end.minusDays(MAX_PERIOD + 1);
        Granularity granularity = mock(Granularity.class);
        var meteringPointsRequest = mock(MeteringPointsRequest.class);
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);
        UUID userCorrelationId = UUID.randomUUID();


        // When
        // Then
        //noinspection ReactiveStreamsUnusedPublisher
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(startExceedsMaxPeriod, end, granularity, meteringPointsRequest, "token", userCorrelationId));
    }

    @ParameterizedTest
    @MethodSource
    void isAlive_returnsMonoWithBoolean(boolean expected) {
        // Given
        IsAliveApi isAliveApi = mock(IsAliveApi.class);
        when(isAliveApi.apiIsaliveGet()).thenReturn(Mono.just(expected));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), null, null, isAliveApi);

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
        TokenApi tokenApi = mock(TokenApi.class);
        when(tokenApi.apiTokenGet()).thenReturn(Mono.just(new StringApiResponse().result("token")));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), tokenApi, null, null);

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
        TokenApi tokenApi = mock(TokenApi.class);
        HttpClientErrorException unauthorized = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        when(tokenApi.apiTokenGet()).thenReturn(Mono.error(unauthorized));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), tokenApi, null, null);

        // When
        Mono<String> result = customerApi.accessToken("refreshToken");

        // Then
        StepVerifier.create(result)
                .expectError(HttpClientErrorException.Unauthorized.class)
                .verify();
    }

    @Test
    void getTimeSeries_returnsConsumptionRecord() {
        // Given
        ZonedDateTime start = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.of(2023, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        MeterDataApi meterDataApi = mock(MeterDataApi.class);
        var document = new MyEnergyDataMarketDocument()
                .periodTimeInterval(
                        new PeriodtimeInterval()
                                .start(start.toString())
                                .end(end.toLocalDate().toString())
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
        when(meterDataApi.apiMeterdataGettimeseriesDateFromDateToAggregationPost(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(data));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), null, meterDataApi, null);

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
        ZonedDateTime start = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.of(2023, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        MeterDataApi meterDataApi = mock(MeterDataApi.class);
        HttpClientErrorException unauthorized = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        when(meterDataApi.apiMeterdataGettimeseriesDateFromDateToAggregationPost(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Mono.error(unauthorized));
        var customerApi = new EnerginetCustomerApiClient(new ApiClient(), null, meterDataApi, null);

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
}