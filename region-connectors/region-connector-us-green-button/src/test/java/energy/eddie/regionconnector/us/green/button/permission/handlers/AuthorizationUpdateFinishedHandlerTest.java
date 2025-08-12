package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.events.UsAuthorizationUpdateFinishedEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestService;
import energy.eddie.regionconnector.us.green.button.services.historical.collection.HistoricalCollectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationUpdateFinishedHandlerTest {
    @Spy
    private final EventBusImpl eventBus = new EventBusImpl();
    @Mock
    private HistoricalCollectionService historicalCollectionService;
    @Mock
    private PermissionRequestService permissionRequestService;
    @Mock
    private UsPermissionRequestRepository repository;
    @InjectMocks
    private AuthorizationUpdateFinishedHandler handler;

    @Test
    void testAccept_forInactivePermissionRequest_doesNotTriggerHistoricalCollection() {
        // Given
        var pr = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(LocalDate.now(ZoneOffset.UTC).plusDays(1))
                .build();
        var event = new UsAuthorizationUpdateFinishedEvent("pid");
        when(historicalCollectionService.persistMetersForPermissionRequest(pr))
                .thenReturn(Flux.just(new MeterReading("pid", "muid", null, PollingStatus.DATA_NOT_READY)));
        when(repository.getByPermissionId("pid")).thenReturn(pr);


        // When
        StepVerifier.create(eventBus.filteredFlux(UsAuthorizationUpdateFinishedEvent.class))
                    .then(() -> eventBus.emit(event))
                    .then(eventBus::close)
                    // Then
                    .expectNextCount(1)
                    .verifyComplete();
        verify(historicalCollectionService, never()).triggerHistoricalDataCollection(any(), any());
    }

    @Test
    void testAccept_historicalCollection_isOnlyTriggeredNotReadyToPollMeters() {
        // Given
        var pr = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(LocalDate.now(ZoneOffset.UTC).minusDays(1))
                .build();
        var event = new UsAuthorizationUpdateFinishedEvent("pid");
        var meter1 = new MeterReading("pid", "muid1", null, PollingStatus.DATA_NOT_READY);
        var meter2 = new MeterReading("pid", "muid2", null, PollingStatus.DATA_READY);
        when(historicalCollectionService.persistMetersForPermissionRequest(pr))
                .thenReturn(Flux.just(meter1, meter2));
        when(historicalCollectionService.triggerHistoricalDataCollection(any(), any()))
                .thenReturn(Mono.just("").then());
        when(repository.getByPermissionId("pid")).thenReturn(pr);

        // When
        handler.accept(event);

        // Then
        verify(historicalCollectionService)
                .triggerHistoricalDataCollection(assertArg(res -> assertEquals(1, res.size())), eq(pr));
        verify(permissionRequestService).removeUnfulfillablePermissionRequest("pid");
    }
}