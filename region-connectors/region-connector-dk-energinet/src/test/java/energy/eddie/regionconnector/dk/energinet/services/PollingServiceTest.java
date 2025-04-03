package energy.eddie.regionconnector.dk.energinet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.EnerginetBeanConfig;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkInternalGranularityEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkInternalPollingEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static energy.eddie.regionconnector.dk.energinet.services.PollingService.REQUESTED_AGGREGATION_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    private final ObjectMapper mapper = new EnerginetBeanConfig().objectMapper();
    @Mock
    private EnerginetCustomerApi customerApi;
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private ValidatedHistoricalDataDataNeed dataNeed;
    @Captor
    private ArgumentCaptor<DkInternalPollingEvent> pollingEventCaptor;
    private PollingService pollingService;

    private static Stream<Arguments> granularityCombinations() {
        return Stream.of(
                Arguments.of(Granularity.PT1H, Granularity.PT1H, Granularity.PT15M, Granularity.P1Y),
                Arguments.of(Granularity.PT15M, Granularity.P1D, Granularity.P1D, Granularity.P1M),
                Arguments.of(Granularity.PT15M, Granularity.P1M, Granularity.P1M, Granularity.P1M),
                Arguments.of(Granularity.PT15M, Granularity.P1Y, Granularity.P1Y, Granularity.P1Y),
                Arguments.of(Granularity.PT1H, Granularity.P1M, Granularity.P1M, Granularity.P1Y)
        );
    }

    @BeforeEach
    @SuppressWarnings("DirectInvocationOnMock")
        // the outbox is not directly invoked
    void setUp() {
        pollingService = new PollingService(
                customerApi,
                new MeterReadingPermissionUpdateAndFulfillmentService(
                        new FulfillmentService(outbox, DkSimpleEvent::new),
                        (reading, end) -> outbox.commit(new DkInternalPollingEvent(reading.permissionId(), end))
                ),
                outbox,
                mapper,
                dataNeedsService,
                new ApiExceptionService(outbox)
        );
    }

    @Test
    void fetchingAMeterReading_revokesPermissionRequest_whenTokenInvalid() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                Granularity.PT1H,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        WebClientResponseException unauthorized = WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(),
                                                                                    "",
                                                                                    HttpHeaders.EMPTY,
                                                                                    null,
                                                                                    null);
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .then(pollingService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
    }

    @Test
    void fetchingAMeterReading_marksPermissionRequestAsUnfulfillable_whenResolutionDoesNotSatisfyDataNeed() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                null,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());
        var document = new MeteringPointDetailsCustomerDtoResponseListApiResponse();
        document.addResultItem(
                new MeteringPointDetailsCustomerDtoResponse()
                        .result(new MeteringPointDetailsCustomerDto()
                                        .meterReadingOccurrence(Granularity.PT1H.name())
                        )
        );
        when(customerApi.getMeteringPointDetails(any(), eq("token")))
                .thenReturn(Mono.just(document));

        when(dataNeed.minGranularity()).thenReturn(Granularity.PT15M);
        when(dataNeed.maxGranularity()).thenReturn(Granularity.PT15M);
        when(dataNeedsService.findById(dataNeedId))
                .thenReturn(Optional.of(dataNeed));

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .then(pollingService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())));
    }

    @ParameterizedTest
    @MethodSource("granularityCombinations")
    // max is not used in all cases, so we need to set it to lenient
    @MockitoSettings(strictness = Strictness.LENIENT)
    void fetchingAMeterReading_emitsExpectedGranularityEvent_ifPermissionContainsNoGranularity(
            Granularity meterReadingOccurrence,
            Granularity expectedGranularity,
            Granularity min,
            Granularity max
    ) {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                null,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());
        var document = new MeteringPointDetailsCustomerDtoResponseListApiResponse();
        document.addResultItem(
                new MeteringPointDetailsCustomerDtoResponse()
                        .result(new MeteringPointDetailsCustomerDto()
                                        .meterReadingOccurrence(meterReadingOccurrence.name())
                        )
        );
        when(customerApi.getMeteringPointDetails(any(), eq("token")))
                .thenReturn(Mono.just(document));
        when(customerApi.getTimeSeries(eq(start),
                                       eq(end.plusDays(1)),
                                       any(),
                                       any(),
                                       eq("token"),
                                       any()))
                .thenReturn(Mono.empty());

        when(dataNeed.minGranularity()).thenReturn(min);
        when(dataNeed.maxGranularity()).thenReturn(max);
        when(dataNeedsService.findById(dataNeedId))
                .thenReturn(Optional.of(dataNeed));

        // When
        StepVerifier.Step<IdentifiableApiResponse> stepVerifier = StepVerifier
                .create(pollingService.identifiableMeterReadings())
                .then(() -> pollingService.fetchHistoricalMeterReadings(
                        permissionRequest))
                .then(pollingService::close);

        // Then
        stepVerifier
                .expectComplete()
                .verify(Duration.ofSeconds(2));
        ArgumentCaptor<DkInternalGranularityEvent> granularityEventCaptor = ArgumentCaptor.forClass(
                DkInternalGranularityEvent.class);
        verify(outbox).commit(granularityEventCaptor.capture());

        assertAll(
                () -> assertEquals(expectedGranularity, granularityEventCaptor.getValue().granularity()),
                () -> assertEquals(permissionRequest.permissionId(), granularityEventCaptor.getValue().permissionId())
        );
    }

    @Test
    void fetchingAMeterReading_returnsAggregationUnavailable_emitsUnfulfillable() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                Granularity.PT15M,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());
        var response = new MyEnergyDataMarketDocumentResponse(REQUESTED_AGGREGATION_UNAVAILABLE)
                .success(false)
                .errorText("error text");
        var document = new MyEnergyDataMarketDocumentResponseListApiResponse()
                .addResultItem(response);

        when(customerApi.getTimeSeries(eq(start),
                                       eq(end.plusDays(1)),
                                       any(),
                                       any(),
                                       eq("token"),
                                       any()))
                .thenReturn(Mono.just(document));

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .then(pollingService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())));
    }

    @Test
    void fetchingAMeterReading_doesNotRevokePermission_on5xx() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                Granularity.PT1H,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        WebClientResponseException unauthorized = WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                                                    "",
                                                                                    HttpHeaders.EMPTY,
                                                                                    null,
                                                                                    null);
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .then(pollingService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox, never()).commit(any());
    }

    @Test
    void fetchingAMeterReading_emitsRecord() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                Granularity.PT1H,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());
        MyEnergyDataMarketDocumentResponse resultItem = new MyEnergyDataMarketDocumentResponse();
        resultItem.setMyEnergyDataMarketDocument(new MyEnergyDataMarketDocument()
                                                         .addTimeSeriesItem(new TimeSeries().addPeriodItem(new Period().resolution(
                                                                 Granularity.PT1H.name())))
                                                         .periodTimeInterval(new PeriodtimeInterval()
                                                                                     .start(start.atStartOfDay(
                                                                                             ZoneOffset.UTC).format(
                                                                                             DateTimeFormatter.ISO_DATE_TIME))
                                                                                     .end(end.atStartOfDay(ZoneOffset.UTC)
                                                                                             .format(DateTimeFormatter.ISO_DATE_TIME)))
        );
        MyEnergyDataMarketDocumentResponseListApiResponse data = new MyEnergyDataMarketDocumentResponseListApiResponse()
                .addResultItem(resultItem);
        when(customerApi.getTimeSeries(eq(start),
                                       eq(end.plusDays(1)),
                                       any(),
                                       any(),
                                       eq("token"),
                                       any()))
                .thenReturn(Mono.just(data));

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .assertNext(mr -> assertAll(
                            () -> assertEquals(permissionRequest.permissionId(), mr.permissionRequest().permissionId()),
                            () -> assertEquals(permissionRequest.connectionId(), mr.permissionRequest().connectionId()),
                            () -> assertEquals(permissionRequest.dataNeedId(), mr.permissionRequest().dataNeedId()),
                            () -> assertNotNull(mr.apiResponse())
                    ))
                    .then(pollingService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox).commit(pollingEventCaptor.capture());
        var res = pollingEventCaptor.getValue();
        assertEquals(end, res.latestMeterReadingEndDate());
    }

    @Test
    void fetchingInactivePermissionRequest_doesNotEmit() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).plusDays(1);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var accessToken = "accessToken";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                Granularity.PT1H,
                accessToken,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );

        // When
        pollingService.fetchHistoricalMeterReadings(permissionRequest);

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                    .then(pollingService::close)
                    .verifyComplete();
    }
}
