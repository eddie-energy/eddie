package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMeterReadingUpdateEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsPollingNotReadyEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.services.PollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private PollingService pollingService;
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptedHandler acceptedHandler;

    @Test
    void acceptEventTriggersPolling() {
        // Given
        // When
        eventBus.emit(new UsSimpleEvent("pid", PermissionProcessStatus.ACCEPTED));

        // Then
        verify(pollingService).poll("pid");
    }

    @Test
    void pollingStatusUpdateEvent_doesNotTriggerPolling() {
        // Given
        // When
        eventBus.emit(new UsMeterReadingUpdateEvent("pid", Map.of()));

        // Then
        verify(pollingService, never()).poll("pid");
    }

    @Test
    void pollingPollingNotReadyEvent_doesNotTriggerPolling() {
        // Given
        // When
        eventBus.emit(new UsPollingNotReadyEvent("pid"));

        // Then
        verify(pollingService, never()).poll("pid");
    }
}