package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenRepository;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExternallyTerminatedHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private OAuthTokenRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private ExternallyTerminatedHandler handler;

    @Test
    void testAccept_deletesCredentials_onExternallyTerminated() {
        // Given
        // When
        eventBus.emit(new UsSimpleEvent("pid", PermissionProcessStatus.EXTERNALLY_TERMINATED));

        // Then
        verify(repository).deleteById("pid");
    }
}