package energy.eddie.regionconnector.cds.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnfulfillableHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private Outbox outbox;
    @InjectMocks
    @SuppressWarnings("unused")
    private UnfulfillableHandler unfulfillableHandler;

    @Test
    void testAccept_emitsRequiresExternalTermination() {
        // Given
        var event = new SimpleEvent("pid", PermissionProcessStatus.UNFULFILLABLE);

        // When
        eventBus.emit(event);

        // Then
        verify(outbox).commit(assertArg(e -> assertAll(
                () -> assertEquals("pid", e.permissionId()),
                () -> assertEquals(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION, e.status())
        )));
    }
}