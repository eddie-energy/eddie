package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.permission.events.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.events.UsAcceptedEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestService;
import energy.eddie.regionconnector.us.green.button.services.historical.collection.HistoricalCollectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private HistoricalCollectionService historicalCollectionService;
    @Mock
    private PermissionRequestService permissionRequestService;
    @Mock
    private UsPermissionRequestRepository repository;

    @Test
    void testAccept_forPermissionRequestForPastData_fetchesMetersAndTriggersHistoricalCollection() {
        // Given
        var acceptedEvent = new UsAcceptedEvent("pid", "1111");
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(2);
        var end = now.minusDays(1);
        var permissionRequest = getPermissionRequest("pid", start, end);
        when(repository.findAllById(List.of("pid")))
                .thenReturn(List.of(permissionRequest));
        var meterReading = getMeterReading("pid");
        when(historicalCollectionService.persistMetersForPermissionRequests(List.of(permissionRequest)))
                .thenReturn(Flux.just(meterReading));
        when(historicalCollectionService.triggerHistoricalDataCollection(List.of(meterReading)))
                .thenReturn(Mono.empty());
        subscribeToEventBus(1);

        // When
        eventBus.emit(acceptedEvent);

        // Then
        verify(historicalCollectionService).triggerHistoricalDataCollection(List.of(meterReading));
    }

    @Test
    void testAccept_forPermissionRequestForFutureData_fetchesMetersButDoesNotTriggerCollection() {
        // Given
        var acceptedEvent = new UsAcceptedEvent("pid", "1111");
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.plusDays(1);
        var end = now.plusDays(2);
        var permissionRequest = getPermissionRequest("pid", start, end);
        when(repository.findAllById(List.of("pid")))
                .thenReturn(List.of(permissionRequest));
        var meterReading = getMeterReading("pid");
        when(historicalCollectionService.persistMetersForPermissionRequests(List.of(permissionRequest)))
                .thenReturn(Flux.just(meterReading));
        subscribeToEventBus(1);

        // When
        eventBus.emit(acceptedEvent);

        // Then
        verify(historicalCollectionService, never()).triggerHistoricalDataCollection(any());
    }

    @Test
    void testAccept_forPermissionRequest_whereMeterReadingHasADifferentIdThanPermissionRequest() {
        // Given
        var acceptedEvent = new UsAcceptedEvent("pid", "1111");
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.plusDays(1);
        var end = now.plusDays(2);
        var permissionRequest = getPermissionRequest("pid", start, end);
        when(repository.findAllById(List.of("pid")))
                .thenReturn(List.of(permissionRequest));
        var meterReading = getMeterReading("pid1");
        when(historicalCollectionService.persistMetersForPermissionRequests(List.of(permissionRequest)))
                .thenReturn(Flux.just(meterReading));
        subscribeToEventBus(1);

        // When
        eventBus.emit(acceptedEvent);

        // Then
        verify(historicalCollectionService, never()).triggerHistoricalDataCollection(any());
    }

    @Test
    void testAccept_forPermissionRequests_fetchesMetersForAllButTriggersOnlyForPastPermissionRequest() {
        // Given
        var acceptedEvent1 = new UsAcceptedEvent("pid1", "1111");
        var acceptedEvent2 = new UsAcceptedEvent("pid2", "2222");
        var now = LocalDate.now(ZoneOffset.UTC);
        var start1 = now.plusDays(1);
        var end1 = now.plusDays(2);
        var start2 = now.minusDays(2);
        var end2 = now.minusDays(1);
        var permissionRequest1 = getPermissionRequest("pid1", start1, end1);
        var permissionRequest2 = getPermissionRequest("pid2", start2, end2);
        when(repository.findAllById(List.of("pid1", "pid2")))
                .thenReturn(List.of(permissionRequest1, permissionRequest2));
        var meterReading1 = getMeterReading("pid1");
        var meterReading2 = getMeterReading("pid2");
        when(historicalCollectionService.persistMetersForPermissionRequests(List.of(permissionRequest1,
                                                                                    permissionRequest2)))
                .thenReturn(Flux.just(meterReading1, meterReading2));
        var mono = Mono.<Void>empty();
        when(historicalCollectionService.triggerHistoricalDataCollection(List.of(meterReading2)))
                .thenReturn(mono);
        subscribeToEventBus(2);

        // When
        eventBus.emit(acceptedEvent1);
        eventBus.emit(acceptedEvent2);

        // Then
        verify(historicalCollectionService).triggerHistoricalDataCollection(List.of(meterReading2));
    }

    private void subscribeToEventBus(int batchSize) {
        new AcceptedHandler(eventBus,
                            getConfiguration(batchSize),
                            historicalCollectionService,
                            permissionRequestService,
                            repository);
    }

    private static GreenButtonConfiguration getConfiguration(int batchSize) {
        return new GreenButtonConfiguration(
                "token",
                "http://localhost",
                Map.of(),
                Map.of(),
                "http://localhost",
                batchSize,
                "secret");
    }

    private static MeterReading getMeterReading(String permissionId) {
        return new MeterReading(permissionId, "mid", null, PollingStatus.DATA_NOT_READY);
    }

    private static GreenButtonPermissionRequest getPermissionRequest(
            String permissionId,
            LocalDate start,
            LocalDate end
    ) {
        return new GreenButtonPermissionRequest(
                permissionId,
                "cid",
                "dnid",
                start,
                end,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                "US",
                "companyId",
                "http://localhost",
                "scope",
                "authId"
        );
    }
}