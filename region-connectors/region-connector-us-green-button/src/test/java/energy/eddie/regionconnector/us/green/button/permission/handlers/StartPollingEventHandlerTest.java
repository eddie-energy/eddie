package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMeterReadingUpdateEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsStartPollingEvent;
import energy.eddie.regionconnector.us.green.button.services.PollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StartPollingEventHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private PollingService pollingService;
    @InjectMocks
    @SuppressWarnings("unused")
    private StartPollingEventHandler startPollingEventHandler;

    public static Stream<Arguments> testOtherEvents_doNotTriggerPolling() {
        return Stream.of(
                Arguments.of(new UsMeterReadingUpdateEvent("pid", List.of())),
                Arguments.of(new UsSimpleEvent("pid", PermissionProcessStatus.ACCEPTED))
        );
    }

    @Test
    void acceptEventTriggersPolling() {
        // Given
        // When
        eventBus.emit(new UsStartPollingEvent("pid"));

        // Then
        verify(pollingService).poll("pid");
    }

    @ParameterizedTest
    @MethodSource
    void testOtherEvents_doNotTriggerPolling(PermissionEvent event) {
        // Given
        // When
        eventBus.emit(event);

        // Then
        verify(pollingService, never()).poll(any());
    }
}