package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HistoricalDataServiceTest {
    @Mock
    private DataApiService dataApiService;

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
                Arguments.of(now.minusYears(1), now.plusYears(1), "1 year: 1 year ago to 1 year in the future")
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
    void fetchAvailableHistoricalData_callsFetchDataForPermissionRequest_withExpectedParams(
            LocalDate start,
            LocalDate end,
            String description
    ) {
        // Given
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end);
        HistoricalDataService historicalDataService = new HistoricalDataService(dataApiService);

        // When
        historicalDataService.fetchAvailableHistoricalData(permissionRequest);

        // Then
        verify(dataApiService).fetchDataForPermissionRequest(permissionRequest, start, end.plusDays(1));
    }

    private static EsPermissionRequest acceptedPermissionRequest(LocalDate start, LocalDate end) {
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
                null,
                PermissionProcessStatus.ACCEPTED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("futureTimeRanges")
    void fetchAvailableHistoricalData_doesNotCallFetchDataForPermissionRequest_withFutureTimeRanges(
            LocalDate start,
            LocalDate end,
            String description
    ) {
        // Given
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end);
        HistoricalDataService historicalDataService = new HistoricalDataService(dataApiService);

        // When
        historicalDataService.fetchAvailableHistoricalData(permissionRequest);

        // Then
        verifyNoInteractions(dataApiService);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("pastToFutureTimeRanges")
    void fetchAvailableHistoricalData_withPermissionRequestFromPastToFuture(
            LocalDate start,
            LocalDate end,
            String description
    ) {
        // Given
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end);
        HistoricalDataService historicalDataService = new HistoricalDataService(dataApiService);

        // When
        historicalDataService.fetchAvailableHistoricalData(permissionRequest);

        // Then
        verify(dataApiService).fetchDataForPermissionRequest(permissionRequest,
                                                             start,
                                                             LocalDate.now(ZONE_ID_SPAIN).minusDays(1));
    }
}
