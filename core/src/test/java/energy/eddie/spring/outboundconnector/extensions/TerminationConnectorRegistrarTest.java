package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.core.services.TerminationRouter;
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
class TerminationConnectorRegistrarTest {
    @Mock
    private TerminationConnector connector;
    @Mock
    private TerminationRouter service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(connector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new TerminationConnectorRegistrar(null, service));
        assertThrows(NullPointerException.class,
                     () -> new TerminationConnectorRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new TerminationConnectorRegistrar(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new TerminationConnectorRegistrar(Optional.of(connector), service);

        // Then
        verify(service).registerTerminationConnector(any());
    }
}