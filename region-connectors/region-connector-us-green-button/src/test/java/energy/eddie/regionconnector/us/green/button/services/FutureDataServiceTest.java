package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.events.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.us.green.button.services.historical.collection.HistoricalCollectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FutureDataServiceTest {
    @Mock
    private PermissionRequestService permissionRequestService;
    @Mock
    private HistoricalCollectionService historicalCollectionService;
    @Mock
    private MeterReadingRepository meterReadingRepository;
    @InjectMocks
    private FutureDataService futureDataService;

    @Test
    void testPollFutureData_doesNotPoll_ifNoPermissionRequestIsActive() {
        // Given
        when(permissionRequestService.findActivePermissionRequests()).thenReturn(List.of());

        // When
        futureDataService.pollFutureData();

        // Then
        verify(historicalCollectionService, never()).triggerHistoricalDataCollection(anyList());
    }

    @Test
    void testPollFutureData_resetsPollStatusOfMeterReadings() {
        // Given
        var today = LocalDate.now(ZoneOffset.UTC);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        when(permissionRequestService.findActivePermissionRequests())
                .thenReturn(List.of(
                        new GreenButtonPermissionRequest(
                                "pid",
                                "cid",
                                "dnid",
                                today,
                                today,
                                Granularity.PT15M,
                                PermissionProcessStatus.ACCEPTED,
                                now,
                                "US",
                                "company",
                                null,
                                null,
                                List.of(new MeterReading("pid", "mid", now, PollingStatus.DATA_READY)),
                                "1111"
                        )
                ));
        when(historicalCollectionService.triggerHistoricalDataCollection(anyList())).thenReturn(Mono.empty());

        // When
        futureDataService.pollFutureData();

        // Then
        verify(meterReadingRepository)
                .updateHistoricalCollectionStatusForMeter(PollingStatus.DATA_NOT_READY, "pid", "mid");
    }
}