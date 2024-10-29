package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.agnostic.outbound.ConnectionStatusMessageOutboundConnector;
import energy.eddie.core.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings("DataFlowIssue")
@ExtendWith(MockitoExtension.class)
class ConnectionStatusMessageRegistrarTest {
    @Mock
    private ConnectionStatusMessageOutboundConnector connector;
    @Mock
    private PermissionService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(connector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new ConnectionStatusMessageRegistrar(null, service));
        assertThrows(NullPointerException.class,
                     () -> new ConnectionStatusMessageRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new ConnectionStatusMessageRegistrar(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new ConnectionStatusMessageRegistrar(Optional.of(connector), service);

        // Then
        verify(service).getConnectionStatusMessageStream();
        verify(connector).setConnectionStatusMessageStream(any());
    }
}