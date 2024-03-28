package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FutureDataServiceTest {

    private final LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
    private final LocalDate yesterday = today.minusDays(1);
    @Mock
    private PermissionRequestService permissionRequestService;
    @Mock
    private DataApiService dataApiService;

    @Test
    void fetchMeteringData_callsFetchDataForPermissionRequest_forWhenivePermissionRequest() {
        // Given
        EsPermissionRequest activePermissionRequest1 = acceptedPermissionRequest(yesterday, today);
        EsPermissionRequest activePermissionRequest2 = acceptedPermissionRequest(yesterday, today);
        EsPermissionRequest inactivePermissionRequest = acceptedPermissionRequest(today, today);

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.getAllAcceptedPermissionRequests()).thenReturn(Stream.of(activePermissionRequest1,
                                                                                               activePermissionRequest2,
                                                                                               inactivePermissionRequest));


        FutureDataService futureDataService = new FutureDataService(permissionRequestService, dataApiService);

        // When
        futureDataService.fetchMeteringData();

        // Then
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest1, yesterday, yesterday);
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest2, yesterday, yesterday);
        verifyNoMoreInteractions(dataApiService);
    }

    private static EsPermissionRequest acceptedPermissionRequest(LocalDate start, LocalDate end) {
        StateBuilderFactory stateBuilderFactory = new StateBuilderFactory(null);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(
                "connectionId",
                "dataNeedId",
                "nif",
                "meteringPointId");
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest("permissionId",
                                                                             permissionRequestForCreation,
                                                                             start,
                                                                             end,
                                                                             Granularity.PT1H,
                                                                             stateBuilderFactory);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, PermissionProcessStatus.ACCEPTED)
                                                         .build());
        permissionRequest.setDistributorCodeAndPointType(DistributorCode.ASEME, 1);
        return permissionRequest;
    }

    @Test
    void fetchMeteringData_usesLastPulledMeterReading_ifBeforeYesterday() {
        // Given
        LocalDate lastPulledMeterReading = yesterday.minusDays(2);
        EsPermissionRequest activePermissionRequest = acceptedPermissionRequest(lastPulledMeterReading.minusDays(2),
                                                                                yesterday);
        activePermissionRequest.updateLatestMeterReadingEndDate(lastPulledMeterReading);

        when(permissionRequestService.getAllAcceptedPermissionRequests()).thenReturn(Stream.of(activePermissionRequest));

        FutureDataService futureDataService = new FutureDataService(permissionRequestService, dataApiService);

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
        EsPermissionRequest activePermissionRequest = acceptedPermissionRequest(yesterday.minusDays(2), yesterday);
        activePermissionRequest.updateLatestMeterReadingEndDate(yesterday);

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.getAllAcceptedPermissionRequests()).thenReturn(Stream.of(activePermissionRequest));

        FutureDataService futureDataService = new FutureDataService(permissionRequestService, dataApiService);

        // When
        futureDataService.fetchMeteringData();

        // Then
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest, yesterday, yesterday);
        verifyNoMoreInteractions(dataApiService);
    }
}
