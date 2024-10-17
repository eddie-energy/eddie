package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.AccountingPointDataNeedResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApiClient;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private Sinks.Many<IdentifiableMeteringData> meteringData;
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
        when(calculationService.calculate("did")).thenReturn(
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
                        .granularity(Granularity.PT30M)
                        .build()
        );
        when(calculationService.calculate("did")).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.PT30M),
                        new Timeframe(now, now),
                        new Timeframe(now, now)
                )
        );

        // When, Then
        assertThrows(IllegalStateException.class, () -> service.poll("pid"));
    }

    @Test
    void testPollEnergyData_correctDataNeedStartAndEnd() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create()
                        .start(now.minusDays(2))
                        .build()
        );
        when(calculationService.calculate("did")).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.PT15M),
                        new Timeframe(now.minusDays(2), now),
                        new Timeframe(now.minusDays(3), now.minusDays(1))
                )
        );

        // When
        service.poll("pid");

        // Then
        verify(apiClient).energy(
                eq("pid"),
                any(),
                eq(FluviusApi.DataServiceType.QUARTER_HOURLY),
                eq(ZonedDateTime.of(now.minusDays(2).atStartOfDay(), ZoneOffset.UTC)),
                eq(DateTimeUtils.endOfDay(now.minusDays(1), ZoneOffset.UTC))
        );
    }

    @Test
    void testPollEnergyData_15minGranularity_callsApiClient() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create()
                        .granularity(Granularity.PT15M)
                        .build()
        );
        when(calculationService.calculate("did")).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.PT15M),
                        new Timeframe(now, now),
                        new Timeframe(now, now)
                )
        );

        // When
        service.poll("pid");

        // Then
        verify(apiClient).energy(eq("pid"), any(), eq(FluviusApi.DataServiceType.QUARTER_HOURLY), any(), any());
    }

    @Test
    void testPollEnergyData_1dayGranularity_callsApiClient() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid")).thenReturn(
                DefaultFluviusPermissionRequestBuilder.create()
                        .granularity(Granularity.P1D)
                        .build()
        );
        when(calculationService.calculate("did")).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.P1D),
                        new Timeframe(now, now),
                        new Timeframe(now, now)
                )
        );

        // When
        service.poll("pid");

        // Then
        verify(apiClient).energy(eq("pid"), any(), eq(FluviusApi.DataServiceType.DAILY), any(), any());
    }

    @Test
    void testPollEnergyData_responseWithData_emitMeteringData() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var permissionRequest = DefaultFluviusPermissionRequestBuilder.create()
                .granularity(Granularity.P1D)
                .build();
        var sampleEnergyResponseModels = createSampleGetEnergyResponseModels();
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        when(calculationService.calculate("did")).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.P1D),
                        new Timeframe(now, now),
                        new Timeframe(now, now)
                )
        );
        when(apiClient.energy(any(), any(), any(), any(), any())).thenReturn(
                createSampleEnergyResponse(sampleEnergyResponseModels)
        );

        // When
        service.poll("pid");

        // Then
        verify(meteringData).emitNext(
                eq(new IdentifiableMeteringData(permissionRequest, sampleEnergyResponseModels)),
                any()
        );
    }

    @Test
    void testPollEnergyData_responseWithoutData_nothingIsEmitted() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var permissionRequest = DefaultFluviusPermissionRequestBuilder.create()
                .granularity(Granularity.P1D)
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        when(calculationService.calculate("did")).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.P1D),
                        new Timeframe(now, now),
                        new Timeframe(now, now)
                )
        );
        when(apiClient.energy(any(), any(), any(), any(), any())).thenReturn(
                createSampleEnergyResponse(null)
        );

        // When
        service.poll("pid");

        // Then
        verify(meteringData).emitNext(
                eq(new IdentifiableMeteringData(permissionRequest, List.of())),
                any()
        );
    }

    private Mono<GetEnergyResponseModelApiDataResponse> createSampleEnergyResponse(List<ElectricityMeterResponseModel> electricityData) {
        return Mono.just(new GetEnergyResponseModelApiDataResponse().data(
                new GetEnergyResponseModel().electricityMeters(
                        electricityData
                )
        ));
    }

    private List<ElectricityMeterResponseModel> createSampleGetEnergyResponseModels() {
        return List.of(new ElectricityMeterResponseModel()
                .meterID("meterId")
                .dailyEnergy(
                        List.of(
                                new EDailyEnergyItemResponseModel()
                                        .timestampStart(OffsetDateTime.now().minusMinutes(15))
                                        .timestampEnd(OffsetDateTime.now())
                                        .measurement(
                                                List.of(new EMeasurementItemResponseModel().unit("kwH").injectionDayValue(5.0))
                                        )
                        )
                )
        );
    }
}