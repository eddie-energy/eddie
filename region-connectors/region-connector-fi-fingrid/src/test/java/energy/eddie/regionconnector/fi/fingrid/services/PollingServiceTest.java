package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.client.FingridApiClient;
import energy.eddie.regionconnector.fi.fingrid.client.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    @Mock
    private EnergyDataService energyDataService;
    @Mock
    private FingridApiClient api;
    @Mock
    private UpdateGranularityService updateGranularityService;
    @Mock
    private DataNeedsService dataNeedsService; //this is needed to mock the PollingService
    @InjectMocks
    private PollingService pollingService;

    public static Stream<Arguments> pollDoesNothing_onFuturePermissionRequests() {
        return Stream.of(
                Arguments.of(LocalDate.now(ZoneOffset.UTC)),
                Arguments.of(LocalDate.now(ZoneOffset.UTC).plusDays(1))
        );
    }

    @ParameterizedTest
    @MethodSource
    void pollDoesNothing_onFuturePermissionRequests(LocalDate start) {
        // Given
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                start,
                LocalDate.now(ZoneOffset.UTC),
                "cid",
                "mid",
                Granularity.PT1H,
                null
        );

        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(energyDataService, never()).publish(pr);
    }

    @Test
    void poll_publishesData() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                start,
                now,
                "cid",
                "mid",
                Granularity.PT1H,
                null
        );
        var resp = Mono.just(new TimeSeriesResponse(null));
        when(api.getTimeSeriesData(eq("mid"), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(resp);

        when(updateGranularityService.updateGranularity(any(), eq(pr)))
                .thenReturn(resp);
        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(energyDataService).publish(pr);
    }

    @Test
    void pollUsesStartDate_ifNoLatestMeterReadingIsAvailable() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                start,
                now,
                "cid",
                "mid",
                Granularity.PT1H,
                null
        );
        var resp = Mono.just(new TimeSeriesResponse(null));
        when(api.getTimeSeriesData(eq("mid"), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(resp);

        when(updateGranularityService.updateGranularity(any(), eq(pr)))
                .thenReturn(resp);
        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(api)
                .getTimeSeriesData("mid",
                                   "cid",
                                   start.atStartOfDay(ZoneOffset.UTC),
                                   DateTimeUtils.endOfDay(now.minusDays(1), ZoneOffset.UTC),
                                   "PT1H",
                                   null);
    }

    @Test
    void pollUsesYesterday_ifEndIsInTheFuture() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                start,
                now.plusDays(10),
                "cid",
                "mid",
                Granularity.PT1H,
                null
        );
        var resp = Mono.just(new TimeSeriesResponse(null));
        when(api.getTimeSeriesData(eq("mid"), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(resp);

        when(updateGranularityService.updateGranularity(any(), eq(pr)))
                .thenReturn(resp);
        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(api)
                .getTimeSeriesData("mid",
                                   "cid",
                                   start.atStartOfDay(ZoneOffset.UTC),
                                   DateTimeUtils.endOfDay(now.minusDays(1), ZoneOffset.UTC),
                                   "PT1H",
                                   null);
    }

    @Test
    void pollUsesLastMeteringData_ifAvailable() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var latestMeteringData = now.minusDays(5).atStartOfDay(ZoneOffset.UTC);
        var end = now.minusDays(1);
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                start,
                end,
                "cid",
                "mid",
                Granularity.PT1H,
                latestMeteringData
        );
        var resp = Mono.just(new TimeSeriesResponse(null));
        when(api.getTimeSeriesData(eq("mid"), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(resp);

        when(updateGranularityService.updateGranularity(any(), eq(pr)))
                .thenReturn(resp);
        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(api)
                .getTimeSeriesData(
                        "mid",
                        "cid",
                        latestMeteringData,
                        DateTimeUtils.endOfDay(end, ZoneOffset.UTC),
                        "PT1H",
                        null
                );
    }

    @Test
    @SuppressWarnings("DirectInvocationOnMock")
        // Mock is not directly invoked, but reference to method is provided
    void pollWithEmptyResponse_doesNotPublish() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var latestMeteringData = now.minusDays(5).atStartOfDay(ZoneOffset.UTC);
        var end = now.minusDays(1);
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                start,
                end,
                "cid",
                "mid",
                Granularity.PT1H,
                latestMeteringData
        );
        var resp = Mono.just(new TimeSeriesResponse(null));
        when(api.getTimeSeriesData(eq("mid"), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(resp);

        when(updateGranularityService.updateGranularity(any(), eq(pr)))
                .thenReturn(Mono.empty());
        when(energyDataService.publish(pr)).thenReturn(e -> energyDataService.publish(e, pr));
        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(energyDataService, never())
                .publish(any(), any());
    }

    @Test
    @SuppressWarnings("DirectInvocationOnMock")
        // Mock is not directly invoked, but reference to method is provided
    void pollWithErrorResponse_doesNotPublish() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var latestMeteringData = now.minusDays(5).atStartOfDay(ZoneOffset.UTC);
        var end = now.minusDays(1);
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                start,
                end,
                "cid",
                "mid",
                Granularity.PT1H,
                latestMeteringData
        );
        when(api.getTimeSeriesData(eq("mid"), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(Mono.error(new RuntimeException()));

        when(energyDataService.publish(pr)).thenReturn(e -> energyDataService.publish(e, pr));

        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(energyDataService, never())
                .publish(any(), any());
    }
}