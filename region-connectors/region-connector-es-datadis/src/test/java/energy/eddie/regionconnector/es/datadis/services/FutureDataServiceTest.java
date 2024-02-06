package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.mockito.Mockito.*;

class FutureDataServiceTest {

    @Test
    void fetchMeteringData_callsFetchDataForPermissionRequest_forActivePermissionRequest() {
        // Arrange
        EsPermissionRequest activePermissionRequest1 = mock(EsPermissionRequest.class);
        ZonedDateTime yesterday = ZonedDateTime.now(ZONE_ID_SPAIN).minusDays(1);
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
        LocalDate expectedYesterday = yesterday.toLocalDate();
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest1, expectedYesterday, expectedYesterday);
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest2, expectedYesterday, expectedYesterday);
        verifyNoMoreInteractions(dataApiService);
    }

    @Test
    void fetchMeteringData_usesLastPulledMeterReading_ifBeforeYesterday() {
        // Arrange
        EsPermissionRequest activePermissionRequest = mock(EsPermissionRequest.class);
        ZonedDateTime yesterday = ZonedDateTime.now(ZONE_ID_SPAIN).minusDays(1);
        ZonedDateTime lastPulledMeterReading = yesterday.minusDays(2);
        when(activePermissionRequest.start()).thenReturn(yesterday.minusDays(2));
        when(activePermissionRequest.lastPulledMeterReading()).thenReturn(Optional.of(lastPulledMeterReading));

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.getAllAcceptedPermissionRequests()).thenReturn(Stream.of(activePermissionRequest));

        DataApiService dataApiService = mock(DataApiService.class);

        FutureDataService futureDataService = new FutureDataService(permissionRequestService, dataApiService);

        // Act
        futureDataService.fetchMeteringData();

        // Assert
        LocalDate expectedYesterday = yesterday.toLocalDate();
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest, lastPulledMeterReading.toLocalDate(), expectedYesterday);
        verifyNoMoreInteractions(dataApiService);
    }

    @Test
    void fetchMeteringData_usesYesterday_ifLastPulledMeterReadingEqualYesterday() {
        // Arrange
        EsPermissionRequest activePermissionRequest = mock(EsPermissionRequest.class);
        ZonedDateTime yesterday = ZonedDateTime.now(ZONE_ID_SPAIN).minusDays(1);
        when(activePermissionRequest.start()).thenReturn(yesterday.minusDays(2));
        when(activePermissionRequest.lastPulledMeterReading()).thenReturn(Optional.of(yesterday));

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.getAllAcceptedPermissionRequests()).thenReturn(Stream.of(activePermissionRequest));

        DataApiService dataApiService = mock(DataApiService.class);

        FutureDataService futureDataService = new FutureDataService(permissionRequestService, dataApiService);

        // Act
        futureDataService.fetchMeteringData();

        // Assert
        LocalDate expectedYesterday = yesterday.toLocalDate();
        verify(dataApiService).fetchDataForPermissionRequest(activePermissionRequest, expectedYesterday, expectedYesterday);
        verifyNoMoreInteractions(dataApiService);
    }
}