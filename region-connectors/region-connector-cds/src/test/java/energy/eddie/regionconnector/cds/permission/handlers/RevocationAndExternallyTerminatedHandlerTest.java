package energy.eddie.regionconnector.cds.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.persistence.OAuthCredentialsRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevocationAndExternallyTerminatedHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private OAuthCredentialsRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private RevocationAndExternallyTerminatedHandler handler;

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"EXTERNALLY_TERMINATED", "REVOKED"})
    void testAccept_removesOAuthCredentials(PermissionProcessStatus status) {
        // Given
        var event = new SimpleEvent("pid", status);

        // When
        eventBus.emit(event);

        // Then
        verify(repository).deleteByPermissionId("pid");
    }

}