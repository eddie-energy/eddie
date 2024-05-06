package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
    private EsPermissionRequestRepository repository;
    @Mock
    private DataApiService dataApiService;

    @Test
    void fetchMeteringData_callsFetchDataForPermissionRequest_forWhenivePermissionRequest() {
        // Given
        EsPermissionRequest activePermissionRequest1 = acceptedPermissionRequest(yesterday, today, null);
        EsPermissionRequest activePermissionRequest2 = acceptedPermissionRequest(yesterday, today, null);
        EsPermissionRequest inactivePermissionRequest = acceptedPermissionRequest(today, today, null);

        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED)).thenReturn(List.of(activePermissionRequest1,
                                                                                           activePermissionRequest2,
                                                                                           inactivePermissionRequest));


        FutureDataService futureDataService = new FutureDataService(repository, dataApiService);

        // When
        futureDataService.fetchMeteringData();

        // Then
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest1, yesterday, yesterday);
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest2, yesterday, yesterday);
        verifyNoMoreInteractions(dataApiService);
    }

    private static EsPermissionRequest acceptedPermissionRequest(
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
                ZonedDateTime.now(ZoneOffset.UTC)
        );
    }

    @Test
    void fetchMeteringData_usesLastPulledMeterReading_ifBeforeYesterday() {
        // Given
        LocalDate lastPulledMeterReading = yesterday.minusDays(2);
        EsPermissionRequest activePermissionRequest = acceptedPermissionRequest(lastPulledMeterReading.minusDays(2),
                                                                                yesterday, lastPulledMeterReading);

        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(activePermissionRequest));

        FutureDataService futureDataService = new FutureDataService(repository, dataApiService);

        // When
        futureDataService.fetchMeteringData();

        // Then
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest,
                                                             lastPulledMeterReading,
                                                             yesterday);
        verifyNoMoreInteractions(dataApiService);
    }

    @Test
    void fetchMeteringData_usesYesterday_ifLastPulledMeterReadingEqualYesterday() {
        // Given
        EsPermissionRequest activePermissionRequest = acceptedPermissionRequest(yesterday.minusDays(2),
                                                                                yesterday,
                                                                                yesterday);

        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED)).thenReturn(List.of(activePermissionRequest));

        FutureDataService futureDataService = new FutureDataService(repository, dataApiService);

        // When
        futureDataService.fetchMeteringData();

        // Then
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest, yesterday, yesterday);
        verifyNoMoreInteractions(dataApiService);
    }
}
