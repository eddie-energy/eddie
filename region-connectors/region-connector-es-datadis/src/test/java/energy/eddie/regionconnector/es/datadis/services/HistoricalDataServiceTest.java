package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import jakarta.annotation.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.mockito.Mockito.*;

class HistoricalDataServiceTest {

    private static Stream<Arguments> pastTimeRanges() {
        LocalDate now = LocalDate.now(ZONE_ID_SPAIN);
        return Stream.of(
                Arguments.of(now.minusDays(20), now.minusDays(10), "10 days: 20 days ago"),
                Arguments.of(now.minusMonths(10), now.minusMonths(9), "1 month: 10 months ago"),
                Arguments.of(now.minusYears(2), now.minusYears(1), "1 year: 2 years ago")
        );
    }

    private static Stream<Arguments> pastToFutureTimeRanges() {
        LocalDate now = LocalDate.now(ZONE_ID_SPAIN);
        return Stream.of(
                Arguments.of(now.minusDays(10), now.plusDays(10), "10 days: 10 days ago to 10 days in the future"),
                Arguments.of(now.minusMonths(9), now.plusMonths(9), "1 month: 9 months ago to 9 months in the future"),
                Arguments.of(now.minusYears(1), now.plusYears(1), "1 year: 1 year ago to 1 year in the future"),
                Arguments.of(now.minusYears(1), null, "1 year: 1 year ago to unlimited future")
        );
    }

    private static Stream<Arguments> futureTimeRanges() {
        LocalDate now = LocalDate.now(ZONE_ID_SPAIN);
        return Stream.of(
                Arguments.of(now, now.plusDays(20), "20 days: now to 20 days in the future"),
                Arguments.of(now.plusDays(10), now.plusDays(20), "10 days: 10 days in the future"),
                Arguments.of(now.plusMonths(9), now.plusMonths(10), "1 month: 9 months in the future"),
                Arguments.of(now.plusYears(1), now.plusYears(2), "1 year: 1 year in the future")
        );
    }


    @ParameterizedTest(name = "{2}")
    @MethodSource("pastTimeRanges")
    void fetchAvailableHistoricalData_callsFetchDataForPermissionRequest_withExpectedParams(LocalDate start, LocalDate end, String description) {
        // Arrange
        DataApiService dataApiService = mock(DataApiService.class);
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.start()).thenReturn(start);
        when(permissionRequest.end()).thenReturn(end);

        HistoricalDataService historicalDataService = new HistoricalDataService(dataApiService);

        // Act
        historicalDataService.fetchAvailableHistoricalData(permissionRequest);

        // Assert
        verify(dataApiService).fetchDataForPermissionRequest(permissionRequest, start, end);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("futureTimeRanges")
    void fetchAvailableHistoricalData_doesNotCallFetchDataForPermissionRequest_withFutureTimeRanges(LocalDate start, LocalDate end, String description) {
        // Arrange
        DataApiService dataApiService = mock(DataApiService.class);
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.start()).thenReturn(start);
        when(permissionRequest.end()).thenReturn(end);

        HistoricalDataService historicalDataService = new HistoricalDataService(dataApiService);

        // Act
        historicalDataService.fetchAvailableHistoricalData(permissionRequest);

        // Assert
        verifyNoInteractions(dataApiService);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("pastToFutureTimeRanges")
    void fetchAvailableHistoricalData_withPermissionRequestFromPastToFuture(LocalDate start, @Nullable LocalDate end, String description) {
        // Arrange
        DataApiService dataApiService = mock(DataApiService.class);
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.start()).thenReturn(start);
        when(permissionRequest.end()).thenReturn(end);

        HistoricalDataService historicalDataService = new HistoricalDataService(dataApiService);

        // Act
        historicalDataService.fetchAvailableHistoricalData(permissionRequest);

        // Assert
        verify(dataApiService).fetchDataForPermissionRequest(permissionRequest, start, LocalDate.now(ZONE_ID_SPAIN).minusDays(1));
    }
}
