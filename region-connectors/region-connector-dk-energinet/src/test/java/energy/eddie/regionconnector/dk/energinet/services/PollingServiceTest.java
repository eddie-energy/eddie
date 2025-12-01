package energy.eddie.regionconnector.dk.energinet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.EnerginetBeanConfig;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkInternalGranularityEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkInternalPollingEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequestBuilder;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static energy.eddie.regionconnector.dk.energinet.services.PollingService.REQUESTED_AGGREGATION_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
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
    private EnergyDataStreams streams;
    @Captor
    private ArgumentCaptor<DkInternalPollingEvent> pollingEventCaptor;
    private PollingService pollingService;

    @BeforeEach
    @SuppressWarnings("DirectInvocationOnMock")
        // the outbox is not directly invoked
    void setUp() {
        streams = new EnergyDataStreams();
        pollingService = new PollingService(
                customerApi,
                new MeterReadingPermissionUpdateAndFulfillmentService(
                        new FulfillmentService(outbox, DkSimpleEvent::new),
                        (reading, end) -> outbox.commit(new DkInternalPollingEvent(reading.permissionId(), end))
                ),
                outbox,
                mapper,
                dataNeedsService,
                new ApiExceptionService(outbox),
                streams
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
        var permissionRequest = new EnerginetPermissionRequestBuilder().setPermissionId(UUID.randomUUID().toString())
                                                                       .setConnectionId(connectionId)
                                                                       .setDataNeedId(dataNeedId)
                                                                       .setMeteringPoint(meteringPoint)
                                                                       .setRefreshToken(refreshToken)
                                                                       .setStart(start)
                                                                       .setEnd(end)
                                                                       .setGranularity(Granularity.PT1H)
                                                                       .setAccessToken(null)
                                                                       .setStatus(PermissionProcessStatus.ACCEPTED)
                                                                       .setCreated(ZonedDateTime.now(DK_ZONE_ID))
                                                                       .build();
        WebClientResponseException unauthorized = WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(),
                                                                                    "",
                                                                                    HttpHeaders.EMPTY,
                                                                                    null,
                                                                                    null);
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // When
        StepVerifier.create(streams.getValidatedHistoricalDataStream())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .then(() -> streams.close())
                    // Then
                    .verifyComplete();

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
        var permissionRequest = new EnerginetPermissionRequestBuilder().setPermissionId(UUID.randomUUID().toString())
                                                                       .setConnectionId(connectionId)
                                                                       .setDataNeedId(dataNeedId)
                                                                       .setMeteringPoint(meteringPoint)
                                                                       .setRefreshToken(refreshToken)
                                                                       .setStart(start)
                                                                       .setEnd(end)
                                                                       .setGranularity(null)
                                                                       .setAccessToken(null)
                                                                       .setStatus(PermissionProcessStatus.ACCEPTED)
                                                                       .setCreated(ZonedDateTime.now(DK_ZONE_ID))
                                                                       .build();
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
        // When
        StepVerifier.create(streams.getValidatedHistoricalDataStream())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .then(() -> streams.close())
                    // Then
                    .verifyComplete();

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
        var permissionRequest = new EnerginetPermissionRequestBuilder().setPermissionId(UUID.randomUUID().toString())
                                                                       .setConnectionId(connectionId)
                                                                       .setDataNeedId(dataNeedId)
                                                                       .setMeteringPoint(meteringPoint)
                                                                       .setRefreshToken(refreshToken)
                                                                       .setStart(start)
                                                                       .setEnd(end)
                                                                       .setGranularity(null)
                                                                       .setAccessToken(null)
                                                                       .setStatus(PermissionProcessStatus.ACCEPTED)
                                                                       .setCreated(ZonedDateTime.now(DK_ZONE_ID))
                                                                       .build();
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
        pollingService.fetchHistoricalMeterReadings(permissionRequest);

        // Then
        var granularityEventCaptor = ArgumentCaptor.forClass(DkInternalGranularityEvent.class);
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
        var permissionRequest = new EnerginetPermissionRequestBuilder().setPermissionId(UUID.randomUUID().toString())
                                                                       .setConnectionId(connectionId)
                                                                       .setDataNeedId(dataNeedId)
                                                                       .setMeteringPoint(meteringPoint)
                                                                       .setRefreshToken(refreshToken)
                                                                       .setStart(start)
                                                                       .setEnd(end)
                                                                       .setGranularity(Granularity.PT15M)
                                                                       .setAccessToken(null)
                                                                       .setStatus(PermissionProcessStatus.ACCEPTED)
                                                                       .setCreated(ZonedDateTime.now(DK_ZONE_ID))
                                                                       .build();
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

        // When
        pollingService.fetchHistoricalMeterReadings(permissionRequest);

        // Then
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
        var permissionRequest = new EnerginetPermissionRequestBuilder().setPermissionId(UUID.randomUUID().toString())
                                                                       .setConnectionId(connectionId)
                                                                       .setDataNeedId(dataNeedId)
                                                                       .setMeteringPoint(meteringPoint)
                                                                       .setRefreshToken(refreshToken)
                                                                       .setStart(start)
                                                                       .setEnd(end)
                                                                       .setGranularity(Granularity.PT1H)
                                                                       .setAccessToken(null)
                                                                       .setStatus(PermissionProcessStatus.ACCEPTED)
                                                                       .setCreated(ZonedDateTime.now(DK_ZONE_ID))
                                                                       .build();
        WebClientResponseException unauthorized = WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                                                    "",
                                                                                    HttpHeaders.EMPTY,
                                                                                    null,
                                                                                    null);
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // When
        pollingService.fetchHistoricalMeterReadings(permissionRequest);

        // Then
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
        var permissionRequest = new EnerginetPermissionRequestBuilder().setPermissionId(UUID.randomUUID().toString())
                                                                       .setConnectionId(connectionId)
                                                                       .setDataNeedId(dataNeedId)
                                                                       .setMeteringPoint(meteringPoint)
                                                                       .setRefreshToken(refreshToken)
                                                                       .setStart(start)
                                                                       .setEnd(end)
                                                                       .setGranularity(Granularity.PT1H)
                                                                       .setAccessToken(null)
                                                                       .setStatus(PermissionProcessStatus.ACCEPTED)
                                                                       .setCreated(ZonedDateTime.now(DK_ZONE_ID))
                                                                       .build();
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());
        MyEnergyDataMarketDocumentResponse resultItem = new MyEnergyDataMarketDocumentResponse();
        resultItem.setMyEnergyDataMarketDocument(new MyEnergyDataMarketDocument()
                                                         .addTimeSeriesItem(new TimeSeries().addPeriodItem(new Period().resolution(
                                                                 Granularity.PT1H.name())))
                                                         .periodTimeInterval(new PeriodtimeInterval()
                                                                                     .start(start.atStartOfDay(
                                                                                             DK_ZONE_ID).format(
                                                                                             DateTimeFormatter.ISO_DATE_TIME))
                                                                                     .end(end.atStartOfDay(DK_ZONE_ID)
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

        // When
        StepVerifier.create(streams.getValidatedHistoricalDataStream())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .then(() -> streams.close())
                    // Then
                    .assertNext(mr -> assertAll(
                            () -> assertEquals(permissionRequest.permissionId(), mr.permissionRequest().permissionId()),
                            () -> assertEquals(permissionRequest.connectionId(), mr.permissionRequest().connectionId()),
                            () -> assertEquals(permissionRequest.dataNeedId(), mr.permissionRequest().dataNeedId()),
                            () -> assertNotNull(mr.apiResponse())
                    ))
                    .verifyComplete();


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
        var permissionRequest = new EnerginetPermissionRequestBuilder().setPermissionId(UUID.randomUUID().toString())
                                                                       .setConnectionId(connectionId)
                                                                       .setDataNeedId(dataNeedId)
                                                                       .setMeteringPoint(meteringPoint)
                                                                       .setRefreshToken(refreshToken)
                                                                       .setStart(start)
                                                                       .setEnd(end)
                                                                       .setGranularity(Granularity.PT1H)
                                                                       .setAccessToken(accessToken)
                                                                       .setStatus(PermissionProcessStatus.ACCEPTED)
                                                                       .setCreated(ZonedDateTime.now(DK_ZONE_ID))
                                                                       .build();

        // When
        StepVerifier.create(streams.getValidatedHistoricalDataStream())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .then(() -> streams.close())
                    // Then
                    .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource
    void isActiveAndNeedsToBeFetched_forInvalidDataNeed_returnsFalse(DataNeed dataNeed) {
        // Given
        var pr = new EnerginetPermissionRequestBuilder()
                .setDataNeedId("dnid")
                .build();
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.ofNullable(dataNeed));

        // When
        var res = pollingService.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertThat(res).isFalse();
    }

    @Test
    void isActiveAndNeedsToBeFetched_inactivePermissionRequest_returnsFalse() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).plusDays(1);
        var pr = new EnerginetPermissionRequestBuilder()
                .setDataNeedId("dnid")
                .setStart(start)
                .build();
        var dn = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT5M,
                Granularity.P1Y
        );
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(dn));

        // When
        var res = pollingService.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertThat(res).isFalse();
    }

    @Test
    void isActiveAndNeedsToBeFetched_alreadyPolledPermissionRequest_returnsFalse() {
        // Given
        var today = LocalDate.now(DK_ZONE_ID);
        var tomorrow = today.plusDays(1);
        var pr = new EnerginetPermissionRequestBuilder()
                .setDataNeedId("dnid")
                .setStart(today)
                .setLatestMeterReadingEndDate(tomorrow)
                .build();
        var dn = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT5M,
                Granularity.P1Y
        );
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(dn));

        // When
        var res = pollingService.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertThat(res).isFalse();
    }

    @ParameterizedTest
    @MethodSource
    void isActiveAndNeedsToBeFetched_readyToPollPermissionRequest_returnsTrue(LocalDate latestMeterReadingEndDate) {
        // Given
        var yesterday = LocalDate.now(DK_ZONE_ID).minusDays(1);
        var pr = new EnerginetPermissionRequestBuilder()
                .setDataNeedId("dnid")
                .setStart(yesterday)
                .setLatestMeterReadingEndDate(latestMeterReadingEndDate)
                .build();
        var dn = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT5M,
                Granularity.P1Y
        );
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(dn));

        // When
        var res = pollingService.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertThat(res).isTrue();
    }

    @SuppressWarnings("unused") // Errorprone has problems with private method source methods
    private static Stream<Arguments> isActiveAndNeedsToBeFetched_readyToPollPermissionRequest_returnsTrue() {
        var today = LocalDate.now(DK_ZONE_ID);
        return Stream.of(
                Arguments.of(today),
                Arguments.of(today.minusDays(1)),
                Arguments.of((Object) null)
        );
    }

    @SuppressWarnings("unused") // Errorprone has problems with private method source methods
    private static Stream<Arguments> isActiveAndNeedsToBeFetched_forInvalidDataNeed_returnsFalse() {
        return Stream.of(
                Arguments.of(new AccountingPointDataNeed()),
                Arguments.of((Object) null)
        );
    }

    @SuppressWarnings("unused") // Errorprone has problems with private method source methods
    private static Stream<Arguments> granularityCombinations() {
        return Stream.of(
                Arguments.of(Granularity.PT1H, Granularity.PT1H, Granularity.PT15M, Granularity.P1Y),
                Arguments.of(Granularity.PT15M, Granularity.P1D, Granularity.P1D, Granularity.P1M),
                Arguments.of(Granularity.PT15M, Granularity.P1M, Granularity.P1M, Granularity.P1M),
                Arguments.of(Granularity.PT15M, Granularity.P1Y, Granularity.P1Y, Granularity.P1Y),
                Arguments.of(Granularity.PT1H, Granularity.P1M, Granularity.P1M, Granularity.P1Y)
        );
    }
}
