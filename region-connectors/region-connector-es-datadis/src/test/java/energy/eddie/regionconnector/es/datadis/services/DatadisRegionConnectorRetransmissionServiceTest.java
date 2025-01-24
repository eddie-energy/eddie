package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.PointType;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatadisRegionConnectorRetransmissionServiceTest {

    public static final ValidatedHistoricalDataDataNeed VALIDATED_HISTORICAL_DATA_DATA_NEED = new ValidatedHistoricalDataDataNeed(
            new RelativeDuration(null, null, null),
            EnergyType.ELECTRICITY,
            Granularity.PT15M,
            Granularity.PT1H
    );
    public static final AccountingPointDataNeed ACCOUNTING_POINT_DATA_NEED = new AccountingPointDataNeed(
            "x", "x", "x", "x", true, null
    );
    private static final String RC_ID = DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID;
    private static final String PERMISSION_ID = "pid";
    private static final String DATA_NEED_ID = "did";
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private EsPermissionRequestRepository repository;
    @Mock
    private DataApi dataApi;
    @Spy
    private Sinks.Many<IdentifiableMeteringData> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Test
    void requestRetransmission_whenPermissionRequestNotFound_returnsPermissionRequestNotFound() {
        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var today = LocalDate.now(ZONE_ID_SPAIN);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.empty());
        var retransmissionService = new DatadisRegionConnectorRetransmissionService(
                repository,
                dataNeedsService,
                dataApi,
                sink
        );
        var request = new RetransmissionRequest(RC_ID, PERMISSION_ID, today, today);

        StepVerifier
                .create(retransmissionService.requestRetransmission(request))
                .assertNext(result -> assertAll(
                        () -> assertInstanceOf(PermissionRequestNotFound.class, result),
                        () -> assertEquals(PERMISSION_ID, result.permissionId()),
                        () -> assertTrue(result.timestamp().isBefore(ZonedDateTime.now(ZONE_ID_SPAIN))),
                        () -> assertTrue(result.timestamp().isAfter(before))
                ))
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"ACCEPTED", "FULFILLED"}, mode = EnumSource.Mode.EXCLUDE)
    void requestRetransmission_forNonAcceptedOrFulfilledRequest_returnsNoActivePermission(PermissionProcessStatus status) {
        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var today = LocalDate.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(today, today, status);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        var retransmissionService = new DatadisRegionConnectorRetransmissionService(
                repository,
                dataNeedsService,
                dataApi,
                sink
        );

        var request = new RetransmissionRequest(RC_ID, PERMISSION_ID, today, today);

        StepVerifier
                .create(retransmissionService.requestRetransmission(request))
                .assertNext(result -> assertAll(
                        () -> assertInstanceOf(NoActivePermission.class, result),
                        () -> assertEquals(PERMISSION_ID, result.permissionId()),
                        () -> assertTrue(result.timestamp().isBefore(ZonedDateTime.now(ZONE_ID_SPAIN))),
                        () -> assertTrue(result.timestamp().isAfter(before))
                ))
                .verifyComplete();
    }


    @Test
    void requestRetransmission_forAccountingPointDataNeed_returnsNotSupported() {
        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var today = LocalDate.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(today, today, PermissionProcessStatus.ACCEPTED);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(ACCOUNTING_POINT_DATA_NEED);
        var retransmissionService = new DatadisRegionConnectorRetransmissionService(
                repository,
                dataNeedsService,
                dataApi,
                sink
        );

        var request = new RetransmissionRequest(RC_ID, PERMISSION_ID, today, today);

        StepVerifier
                .create(retransmissionService.requestRetransmission(request))
                .assertNext(result -> assertAll(
                        () -> assertInstanceOf(NotSupported.class, result),
                        () -> assertEquals(PERMISSION_ID, result.permissionId()),
                        () -> {
                            if (result instanceof NotSupported notSupported) {
                                assertEquals("Retransmission of data for AccountingPointDataNeed not supported",
                                             notSupported.reason());
                            }
                        },
                        () -> assertTrue(result.timestamp().isBefore(ZonedDateTime.now(ZONE_ID_SPAIN))),
                        () -> assertTrue(result.timestamp().isAfter(before))
                ))
                .verifyComplete();
    }

    @SuppressWarnings("unused")
    @ParameterizedTest
    @MethodSource("retransmissionOutsidePermissionTimeFrame")
    void requestRetransmission_whenRetransmissionRequestOutsideOfPermissionTimeFrame_returnsNoPermissionForTimeFrame(
            LocalDate permissionStart,
            LocalDate permissionEnd,
            LocalDate retransmissionFrom,
            LocalDate retransmissionTo,
            String reason
    ) {
        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(permissionStart, permissionEnd, PermissionProcessStatus.ACCEPTED);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(VALIDATED_HISTORICAL_DATA_DATA_NEED);
        var retransmissionService = new DatadisRegionConnectorRetransmissionService(
                repository,
                dataNeedsService,
                dataApi,
                sink
        );

        var request = new RetransmissionRequest(RC_ID, PERMISSION_ID, retransmissionFrom, retransmissionTo);

        StepVerifier
                .create(retransmissionService.requestRetransmission(request))
                .assertNext(result -> assertAll(
                        () -> assertInstanceOf(NoPermissionForTimeFrame.class, result),
                        () -> assertEquals(PERMISSION_ID, result.permissionId()),
                        () -> assertTrue(result.timestamp().isBefore(ZonedDateTime.now(ZONE_ID_SPAIN))),
                        () -> assertTrue(result.timestamp().isAfter(before))
                ))
                .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("retransmissionToInvalidDate")
    void requestRetransmission_retransmissionToIsToday_returnsNotSupported(LocalDate retransmissionTo) {
        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var today = LocalDate.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(today.minusWeeks(1),
                                                  today.plusMonths(1),
                                                  PermissionProcessStatus.ACCEPTED);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(VALIDATED_HISTORICAL_DATA_DATA_NEED);
        var retransmissionService = new DatadisRegionConnectorRetransmissionService(
                repository,
                dataNeedsService,
                dataApi,
                sink
        );

        var request = new RetransmissionRequest(RC_ID, PERMISSION_ID, today.minusDays(1), retransmissionTo);

        StepVerifier
                .create(retransmissionService.requestRetransmission(request))
                .assertNext(result -> assertAll(
                        () -> assertInstanceOf(NotSupported.class, result),
                        () -> assertEquals(PERMISSION_ID, result.permissionId()),
                        () -> {
                            if (result instanceof NotSupported notSupported) {
                                assertEquals("Retransmission to date needs to be before today",
                                             notSupported.reason());
                            }
                        },
                        () -> assertTrue(result.timestamp().isBefore(ZonedDateTime.now(ZONE_ID_SPAIN))),
                        () -> assertTrue(result.timestamp().isAfter(before))
                ))
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"FULFILLED", "ACCEPTED"})
    void requestRetransmission_fetchesData_emitsExpectedDataAndReturnsSuccess(PermissionProcessStatus status) throws IOException {
        var meteringData = MeteringDataProvider.loadMeteringData();
        var meteringDataStart = meteringData.getFirst().date();
        var meteringDataEnd = meteringData.getLast().date();
        var retransmitFrom = meteringDataStart.plusDays(2);
        var retransmitTo = retransmitFrom.plusDays(2);

        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(meteringDataStart,
                                                  meteringDataEnd.plusWeeks(1),
                                                  status);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(VALIDATED_HISTORICAL_DATA_DATA_NEED);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.just(meteringData));
        var retransmissionService = new DatadisRegionConnectorRetransmissionService(
                repository,
                dataNeedsService,
                dataApi,
                sink
        );

        var request = new RetransmissionRequest(RC_ID, PERMISSION_ID, retransmitFrom, retransmitTo);

        var emitVerifier = StepVerifier
                .create(sink.asFlux())
                .assertNext(result -> assertAll(
                        () -> assertEquals(retransmitFrom, result.payload().start()),
                        () -> assertEquals(retransmitTo, result.payload().end()),
                        () -> assertEquals(PERMISSION_ID, result.permissionRequest().permissionId())
                ))
                .then(() -> sink.tryEmitComplete());

        StepVerifier
                .create(retransmissionService.requestRetransmission(request))
                .assertNext(result -> assertAll(
                        () -> assertInstanceOf(Success.class, result),
                        () -> assertEquals(PERMISSION_ID, result.permissionId()),
                        () -> assertTrue(result.timestamp().isBefore(ZonedDateTime.now(ZONE_ID_SPAIN))),
                        () -> assertTrue(result.timestamp().isAfter(before))
                ))
                .verifyComplete();

        emitVerifier.verifyComplete();
    }

    @Test
    void requestRetransmission_fetchesDataEmitFails_returnsFailure() throws IOException {
        var meteringData = MeteringDataProvider.loadMeteringData();
        var meteringDataStart = meteringData.getFirst().date();
        var meteringDataEnd = meteringData.getLast().date();
        var retransmitFrom = meteringDataStart.plusDays(2);
        var retransmitTo = retransmitFrom.plusDays(2);

        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(meteringDataStart,
                                                  meteringDataEnd.plusWeeks(1),
                                                  PermissionProcessStatus.ACCEPTED);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(VALIDATED_HISTORICAL_DATA_DATA_NEED);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.just(meteringData));
        var retransmissionService = new DatadisRegionConnectorRetransmissionService(
                repository,
                dataNeedsService,
                dataApi,
                sink
        );

        var request = new RetransmissionRequest(RC_ID, PERMISSION_ID, retransmitFrom, retransmitTo);
        sink.tryEmitComplete();

        StepVerifier
                .create(retransmissionService.requestRetransmission(request))
                .assertNext(result -> assertAll(
                        () -> assertInstanceOf(Failure.class, result),
                        () -> assertEquals(PERMISSION_ID, result.permissionId()),
                        () -> {
                            if (result instanceof Failure failure) {
                                assertEquals("Could not emit fetched data", failure.reason());
                            }
                        },
                        () -> assertTrue(result.timestamp().isBefore(ZonedDateTime.now(ZONE_ID_SPAIN))),
                        () -> assertTrue(result.timestamp().isAfter(before))
                ))
                .verifyComplete();
    }


    @Test
    void requestRetransmission_fetchesEmptyData_emitsDataNotAvailable() {
        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var today = LocalDate.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(today.minusWeeks(1),
                                                  today.plusWeeks(1),
                                                  PermissionProcessStatus.ACCEPTED);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(VALIDATED_HISTORICAL_DATA_DATA_NEED);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.just(List.of()));
        var retransmissionService = new DatadisRegionConnectorRetransmissionService(
                repository,
                dataNeedsService,
                dataApi,
                sink
        );

        var request = new RetransmissionRequest(RC_ID, PERMISSION_ID, today.minusDays(1), today.minusDays(1));

        StepVerifier
                .create(retransmissionService.requestRetransmission(request))
                .assertNext(result -> assertAll(
                        () -> assertInstanceOf(DataNotAvailable.class, result),
                        () -> assertEquals(PERMISSION_ID, result.permissionId()),
                        () -> assertTrue(result.timestamp().isBefore(ZonedDateTime.now(ZONE_ID_SPAIN))),
                        () -> assertTrue(result.timestamp().isAfter(before))
                ))
                .verifyComplete();
    }

    @Test
    void requestRetransmission_fetchesDataButRequestedDataNotReturned_emitsDataNotAvailable() throws IOException {
        var meteringData = MeteringDataProvider.loadMeteringData();
        var meteringDataStart = meteringData.getFirst().date();
        var meteringDataEnd = meteringData.getLast().date();

        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(meteringDataStart,
                                                  meteringDataEnd.plusWeeks(2),
                                                  PermissionProcessStatus.ACCEPTED);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(VALIDATED_HISTORICAL_DATA_DATA_NEED);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.just(meteringData));
        var retransmissionService = new DatadisRegionConnectorRetransmissionService(
                repository,
                dataNeedsService,
                dataApi,
                sink
        );

        var request = new RetransmissionRequest(RC_ID,
                                                PERMISSION_ID,
                                                meteringDataEnd.plusDays(1),
                                                meteringDataEnd.plusWeeks(1));

        StepVerifier
                .create(retransmissionService.requestRetransmission(request))
                .assertNext(result -> assertAll(
                        () -> assertInstanceOf(DataNotAvailable.class, result),
                        () -> assertEquals(PERMISSION_ID, result.permissionId()),
                        () -> assertTrue(result.timestamp().isBefore(ZonedDateTime.now(ZONE_ID_SPAIN))),
                        () -> assertTrue(result.timestamp().isAfter(before))
                ))
                .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("apiErrors")
    void requestRetransmission_fetchErrors_emitsFailure(Throwable throwable, String expectedReason) {
        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var today = LocalDate.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(today.minusWeeks(1),
                                                  today.plusWeeks(1),
                                                  PermissionProcessStatus.ACCEPTED);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(VALIDATED_HISTORICAL_DATA_DATA_NEED);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.error(throwable));

        var retransmissionService = new DatadisRegionConnectorRetransmissionService(
                repository,
                dataNeedsService,
                dataApi,
                sink
        );

        var request = new RetransmissionRequest(RC_ID, PERMISSION_ID, today.minusDays(1), today.minusDays(1));

        StepVerifier
                .create(retransmissionService.requestRetransmission(request))
                .assertNext(result -> assertAll(
                        () -> assertInstanceOf(Failure.class, result),
                        () -> assertEquals(PERMISSION_ID, result.permissionId()), () -> {
                            if (result instanceof Failure failure) {
                                assertEquals(expectedReason, failure.reason());
                            }
                        },
                        () -> assertTrue(result.timestamp().isBefore(ZonedDateTime.now(ZONE_ID_SPAIN))),
                        () -> assertTrue(result.timestamp().isAfter(before))
                ))
                .verifyComplete();
    }

    private static Stream<Arguments> retransmissionOutsidePermissionTimeFrame() {
        var today = LocalDate.now(ZONE_ID_SPAIN);
        var startDate = today.minusDays(5);
        var endDate = today.plusDays(5);
        return Stream.of(
                Arguments.of(startDate,
                             endDate,
                             startDate.minusDays(2),
                             startDate.minusDays(1),
                             "completely before start"),
                Arguments.of(startDate, endDate, startDate.minusDays(1), endDate.minusDays(1), "from before start"),
                Arguments.of(startDate, endDate, startDate.plusDays(1), endDate.plusDays(1), "to after end"),
                Arguments.of(startDate, endDate, endDate.plusDays(1), endDate.plusDays(2), "completely after end"),
                Arguments.of(startDate,
                             endDate,
                             startDate.minusDays(1),
                             endDate.plusDays(1),
                             "from before start & to after end")
        );
    }

    private static Stream<Arguments> retransmissionToInvalidDate() {
        var today = LocalDate.now(ZONE_ID_SPAIN);
        return Stream.of(
                Arguments.of(today),
                Arguments.of(today.plusDays(1)),
                Arguments.of(today.plusWeeks(1))
        );
    }

    private static Stream<Arguments> apiErrors() {
        var forbidden = new DatadisApiException("FORBIDDEN", HttpResponseStatus.FORBIDDEN, "");
        return Stream.of(
                Arguments.of(new Throwable("reason"), "reason"),
                Arguments.of(new DatadisApiException("", HttpResponseStatus.TOO_MANY_REQUESTS, ""),
                             "Datadis returned: '429 Too Many Requests'. Try again in 24 hours."),
                Arguments.of(forbidden, forbidden.getMessage())
        );
    }

    private static EsPermissionRequest permissionRequest(
            LocalDate start,
            LocalDate end,
            PermissionProcessStatus status
    ) {
        return new DatadisPermissionRequestBuilder()
                .setPermissionId(PERMISSION_ID)
                .setDataNeedId(DATA_NEED_ID)
                .setStart(start)
                .setEnd(end)
                .setStatus(status)
                .setDistributorCode(DistributorCode.ASEME)
                .setGranularity(Granularity.PT15M)
                .setPointType(PointType.TYPE_1)
                .build();
    }
}