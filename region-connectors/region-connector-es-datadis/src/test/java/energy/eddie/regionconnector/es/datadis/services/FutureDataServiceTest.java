package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.mockito.Mockito.*;

class FutureDataServiceTest {

    @Test
    void fetchMeteringData_callsFetchDataForPermissionRequest_forActivePermissionRequest() {
        // Arrange
        EsPermissionRequest activePermissionRequest1 = mock(EsPermissionRequest.class);
        LocalDate yesterday = LocalDate.now(ZONE_ID_SPAIN).minusDays(1);
        when(activePermissionRequest1.start()).thenReturn(yesterday);
        when(activePermissionRequest1.lastPulledMeterReading()).thenReturn(Optional.empty());
        EsPermissionRequest activePermissionRequest2 = mock(EsPermissionRequest.class);
        when(activePermissionRequest2.start()).thenReturn(yesterday);
        when(activePermissionRequest2.lastPulledMeterReading()).thenReturn(Optional.empty());
        EsPermissionRequest inactivePermissionRequest = mock(EsPermissionRequest.class);
        when(inactivePermissionRequest.start()).thenReturn(yesterday.plusDays(1));

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.getAllAcceptedPermissionRequests()).thenReturn(Stream.of(activePermissionRequest1, activePermissionRequest2, inactivePermissionRequest));


        DataApiService dataApiService = mock(DataApiService.class);

        FutureDataService futureDataService = new FutureDataService(permissionRequestService, dataApiService);

        // Act
        futureDataService.fetchMeteringData();

        // Assert
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest1, yesterday, yesterday);
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest2, yesterday, yesterday);
        verifyNoMoreInteractions(dataApiService);
    }

    @Test
    void fetchMeteringData_usesLastPulledMeterReading_ifBeforeYesterday() {
        // Arrange
        EsPermissionRequest activePermissionRequest = mock(EsPermissionRequest.class);
        LocalDate yesterday = LocalDate.now(ZONE_ID_SPAIN).minusDays(1);
        LocalDate lastPulledMeterReading = yesterday.minusDays(2);
        when(activePermissionRequest.start()).thenReturn(yesterday.minusDays(2));
        when(activePermissionRequest.lastPulledMeterReading()).thenReturn(Optional.of(lastPulledMeterReading));

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.getAllAcceptedPermissionRequests()).thenReturn(Stream.of(activePermissionRequest));

        DataApiService dataApiService = mock(DataApiService.class);

        FutureDataService futureDataService = new FutureDataService(permissionRequestService, dataApiService);

        // Act
        futureDataService.fetchMeteringData();

        // Assert
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest, lastPulledMeterReading, yesterday);
        verifyNoMoreInteractions(dataApiService);
    }

    @Test
    void fetchMeteringData_usesYesterday_ifLastPulledMeterReadingEqualYesterday() {
        // Arrange
        EsPermissionRequest activePermissionRequest = mock(EsPermissionRequest.class);
        LocalDate yesterday = LocalDate.now(ZONE_ID_SPAIN).minusDays(1);
        when(activePermissionRequest.start()).thenReturn(yesterday.minusDays(2));
        when(activePermissionRequest.lastPulledMeterReading()).thenReturn(Optional.of(yesterday));

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.getAllAcceptedPermissionRequests()).thenReturn(Stream.of(activePermissionRequest));

        DataApiService dataApiService = mock(DataApiService.class);

        FutureDataService futureDataService = new FutureDataService(permissionRequestService, dataApiService);

        // Act
        futureDataService.fetchMeteringData();

        // Assert
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest, yesterday, yesterday);
        verifyNoMoreInteractions(dataApiService);
    }
}
