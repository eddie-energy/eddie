package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.us.green.button.services.historical.collection.HistoricalCollectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

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
        verify(historicalCollectionService, never()).triggerHistoricalDataCollection(anyList(), any());
    }

    @Test
    void testPollFutureData_resetsPollStatusOfMeterReadings() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setLastMeterReadings(List.of(new MeterReading("pid",
                                                                                                         "mid",
                                                                                                         now,
                                                                                                         PollingStatus.DATA_READY)))
                                                          .build();
        when(permissionRequestService.findActivePermissionRequests()).thenReturn(List.of(pr));
        when(historicalCollectionService.triggerHistoricalDataCollection(pr)).thenReturn(Mono.empty());

        // When
        futureDataService.pollFutureData();

        // Then
        verify(meterReadingRepository)
                .updateHistoricalCollectionStatusForMeter(PollingStatus.DATA_NOT_READY, "pid", "mid");
    }
}