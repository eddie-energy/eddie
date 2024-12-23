package energy.eddie.regionconnector.be.fluvius.service.polling;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.AccountingPointDataNeedResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.clients.DataServiceType;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApiClient;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    @Mock
    private BePermissionRequestRepository repository;
    @Mock
    private FluviusApiClient apiClient;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @Mock
    private IdentifiableDataStreams streams;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private PollingService service;

    @Test
    void testPollEnergyData_notStarted_noDataIsFetched() {
        // Given
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create()
                                                      .start(LocalDate.now(ZoneOffset.UTC).plusDays(1))
                                                      .build()
        );

        // When
        service.poll("pid");

        // Then
        verify(apiClient, never()).energy(any(), any(), any(), any(), any());
    }

    @Test
    void testPollEnergyData_noVhdDataNeed_noDataIsFetched() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create().build()
        );
        when(calculationService.calculate(eq("did"), any()))
                .thenReturn(
                        new AccountingPointDataNeedResult(new Timeframe(now, now))
                );

        // When
        service.poll("pid");

        // Then
        verify(apiClient, never()).energy(any(), any(), any(), any(), any());
    }

    @Test
    void testPollEnergyData_illegalGranularity_throw() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create()
                                                      .granularity(Granularity.PT5M)
                                                      .addMeterReadings(new MeterReading("pid", "ean", null))
                                                      .dataNeedId("did")
                                                      .build()
        );
        when(calculationService.calculate(eq("did"), any()))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeedResult(
                                List.of(Granularity.PT30M),
                                new Timeframe(now, now),
                                new Timeframe(now, now)
                        )
                );

        // When, Then
        assertThrows(IllegalArgumentException.class, () -> service.poll("pid"));
    }

    @Test
    void testPollEnergyData_correctDataNeedStartAndEnd() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create()
                                                      .start(now.minusDays(2))
                                                      .addMeterReadings(new MeterReading("pid", "ean", null))
                                                      .dataNeedId("did")
                                                      .build()
        );
        when(calculationService.calculate(eq("did"), any()))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeedResult(
                                List.of(Granularity.PT15M),
                                new Timeframe(now.minusDays(2), now),
                                new Timeframe(now.minusDays(3), now.minusDays(1))
                        )
                );
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.just(new GetEnergyResponseModelApiDataResponse()));

        // When
        service.poll("pid");

        // Then
        verify(apiClient).energy(
                eq("pid"),
                any(),
                eq(DataServiceType.QUARTER_HOURLY),
                eq(ZonedDateTime.of(now.minusDays(2).atStartOfDay(), ZoneOffset.UTC)),
                eq(now.atStartOfDay(ZoneOffset.UTC))
        );
    }

    @Test
    void testPollEnergyData_15minGranularity_callsApiClient() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create()
                                                      .granularity(Granularity.PT15M)
                                                      .addMeterReadings(new MeterReading("pid", "ean", null))
                                                      .dataNeedId("did")
                                                      .build()
        );
        when(calculationService.calculate(eq("did"), any()))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeedResult(
                                List.of(Granularity.PT15M),
                                new Timeframe(now, now),
                                new Timeframe(now, now)
                        )
                );
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.just(new GetEnergyResponseModelApiDataResponse()));

        // When
        service.poll("pid");

        // Then
        verify(apiClient).energy(eq("pid"), any(), eq(DataServiceType.QUARTER_HOURLY), any(), any());
    }

    @Test
    void testPollEnergyData_retriesOnError() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create()
                                                      .granularity(Granularity.PT15M)
                                                      .addMeterReadings(new MeterReading("pid", "ean", null))
                                                      .dataNeedId("did")
                                                      .build()
        );
        when(calculationService.calculate(eq("did"), any()))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeedResult(
                                List.of(Granularity.PT15M),
                                new Timeframe(now, now),
                                new Timeframe(now, now)
                        )
                );
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.TOO_MANY_REQUESTS.value(),
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null)))
                .thenReturn(Mono.just(new GetEnergyResponseModelApiDataResponse()));

        // When
        service.poll("pid");

        // Then
        verify(apiClient).energy(eq("pid"), any(), eq(DataServiceType.QUARTER_HOURLY), any(), any());
    }

    @Test
    void testPollEnergyData_1dayGranularity_callsApiClient() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder
                        .create()
                        .dataNeedId("did")
                        .granularity(Granularity.P1D)
                        .addMeterReadings(new MeterReading("pid", "ean", null))
                        .build()
        );
        when(calculationService.calculate(eq("did"), any()))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeedResult(
                                List.of(Granularity.P1D),
                                new Timeframe(now, now),
                                new Timeframe(now, now)
                        )
                );
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.DAILY), any(), any()))
                .thenReturn(Mono.just(new GetEnergyResponseModelApiDataResponse()));

        // When
        service.poll("pid");

        // Then
        verify(apiClient).energy(eq("pid"), any(), eq(DataServiceType.DAILY), any(), any());
    }

    @Test
    void testPollEnergyData_responseWithData_emitMeteringData() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var permissionRequest = DefaultFluviusPermissionRequestBuilder
                .create()
                .dataNeedId("did")
                .granularity(Granularity.P1D)
                .addMeterReadings(new MeterReading("pid", "ean", null))
                .build();
        var sampleEnergyResponseModels = createSampleGetEnergyResponseModels();
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        when(calculationService.calculate(eq("did"), any()))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeedResult(
                                List.of(Granularity.P1D),
                                new Timeframe(now, now),
                                new Timeframe(now, now)
                        )
                );
        when(apiClient.energy(any(), any(), any(), any(), any())).thenReturn(Mono.just(sampleEnergyResponseModels));

        // When
        service.poll("pid");

        // Then
        verify(streams).publish(permissionRequest, sampleEnergyResponseModels);
    }

    @Test
    void testPollEnergyData_responseWithoutData_nothingIsEmitted() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var permissionRequest = DefaultFluviusPermissionRequestBuilder
                .create()
                .dataNeedId("did")
                .granularity(Granularity.P1D)
                .addMeterReadings(new MeterReading("pid", "ean", null))
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        when(calculationService.calculate(eq("did"), any()))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeedResult(
                                List.of(Granularity.P1D),
                                new Timeframe(now, now),
                                new Timeframe(now, now)
                        )
                );
        when(apiClient.energy(any(), any(), any(), any(), any()))
                .thenReturn(
                        Mono.just(
                                new GetEnergyResponseModelApiDataResponse()
                                        .data(new GetEnergyResponseModel().electricityMeters(null))
                        )
                );

        // When
        service.poll("pid");

        // Then
        verify(streams, never()).publish(permissionRequest, new GetEnergyResponseModelApiDataResponse());
    }

    @Test
    void testPollEnergyData_revokesPermissionOnForbidden() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create()
                                                      .granularity(Granularity.PT15M)
                                                      .addMeterReadings(new MeterReading("pid", "ean", null))
                                                      .dataNeedId("did")
                                                      .build()
        );
        when(calculationService.calculate(eq("did"), any()))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeedResult(
                                List.of(Granularity.PT15M),
                                new Timeframe(now, now),
                                new Timeframe(now, now)
                        )
                );
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(),
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null)));

        // When
        service.poll("pid");

        // Then
        verify(outbox).commit(assertArg(res -> assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals(PermissionProcessStatus.REVOKED, res.status())
        )));
    }

    @Test
    void testPollEnergyData_forUnrelatedError_doesNotRevoke() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create()
                                                      .granularity(Granularity.PT15M)
                                                      .addMeterReadings(new MeterReading("pid", "ean", null))
                                                      .dataNeedId("did")
                                                      .build()
        );
        when(calculationService.calculate(eq("did"), any()))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeedResult(
                                List.of(Granularity.PT15M),
                                new Timeframe(now, now),
                                new Timeframe(now, now)
                        )
                );
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null)));

        // When
        service.poll("pid");

        // Then
        verify(outbox, never()).commit(any());
    }

    private GetEnergyResponseModelApiDataResponse createSampleGetEnergyResponseModels() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        return new GetEnergyResponseModelApiDataResponse()
                .data(new GetEnergyResponseModel()
                              .addElectricityMetersItem(
                                      new ElectricityMeterResponseModel()
                                              .meterID("meterId")
                                              .addDailyEnergyItem(
                                                      new EDailyEnergyItemResponseModel()
                                                              .timestampStart(now.minusMinutes(15))
                                                              .timestampEnd(now)
                                                              .addMeasurementItem(
                                                                      new EMeasurementItemResponseModel()
                                                                              .unit("kwH")
                                                                              .injectionDayValue(5.0)
                                                              )
                                              )
                              )

                );
    }
}