package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.dtos.AllowedGranularity;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FutureDataServiceTest {

    private final LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
    private final LocalDate yesterday = today.minusDays(1);
    @Mock
    private DataApi dataApi;
    @Mock
    private EsPermissionRequestRepository repository;
    @Spy
    @InjectMocks
    private DataApiService dataApiService;
    @Mock
    DatadisRegionConnectorMetadata metadata;
    CommonFutureDataService<EsPermissionRequest> futureDataService;

    @BeforeEach
    void setUp() {
        when(metadata.timeZone()).thenReturn(ZoneId.of("Europe/Madrid"));
        futureDataService = new CommonFutureDataService<>(dataApiService, repository, "0 0 17 * * *", metadata);
    }

    @Test
    void fetchMeteringData_callsFetchDataForPermissionRequest_forWhenivePermissionRequest() {
        // Given
        EsPermissionRequest activePermissionRequest1 = acceptedPermissionRequest(yesterday, today, null);
        EsPermissionRequest activePermissionRequest2 = acceptedPermissionRequest(yesterday, today, null);
        EsPermissionRequest inactivePermissionRequest = acceptedPermissionRequest(today, today, null);

        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED)).thenReturn(List.of(activePermissionRequest1,
                activePermissionRequest2,
                inactivePermissionRequest));
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.empty());

        // When
        futureDataService.fetchMeterData();

        // Then
        verify(dataApiService).pollTimeSeriesData(activePermissionRequest1);
        verify(dataApiService).pollTimeSeriesData(activePermissionRequest2);
    }

    private static DatadisPermissionRequest acceptedPermissionRequest(
            LocalDate start, LocalDate end,
            LocalDate latestMeterReading
    ) {
        return new DatadisPermissionRequest(
                "permissionId",
                "connectionId",
                "dataNeedId",
                Granularity.PT1H,
                "nif",
                "meteringPointId",
                start,
                end,
                DistributorCode.ASEME,
                1,
                latestMeterReading,
                PermissionProcessStatus.ACCEPTED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC),
                AllowedGranularity.PT15M_OR_PT1H);
    }

    @Test
    void fetchMeteringData_usesLastPulledMeterReading_ifBeforeYesterday() {
        // Given
        LocalDate lastPulledMeterReading = yesterday.minusDays(2);
        EsPermissionRequest activePermissionRequest = acceptedPermissionRequest(lastPulledMeterReading.minusDays(2),
                yesterday, lastPulledMeterReading);

        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(activePermissionRequest));
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.empty());

        // When
        futureDataService.fetchMeterData();

        // Then
        verify(dataApiService).isActiveAndNeedsToBeFetched(activePermissionRequest);
        verify(dataApiService).pollTimeSeriesData(activePermissionRequest);
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest, lastPulledMeterReading, yesterday);
        verifyNoMoreInteractions(dataApiService);
    }

    @Test
    void fetchMeteringData_usesYesterday_ifLastPulledMeterReadingEqualYesterday() {
        // Given
        EsPermissionRequest activePermissionRequest = acceptedPermissionRequest(yesterday.minusDays(2),
                yesterday,
                yesterday);

        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED)).thenReturn(List.of(activePermissionRequest));
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.empty());

        // When
        futureDataService.fetchMeterData();

        // Then
        verify(dataApiService).isActiveAndNeedsToBeFetched(activePermissionRequest);
        verify(dataApiService).pollTimeSeriesData(activePermissionRequest);
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest, yesterday, yesterday);
        verifyNoMoreInteractions(dataApiService);
    }
}
