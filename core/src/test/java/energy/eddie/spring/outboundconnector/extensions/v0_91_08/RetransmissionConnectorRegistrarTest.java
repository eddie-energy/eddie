package energy.eddie.spring.outboundconnector.extensions.v0_91_08;

import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.core.services.CoreRetransmissionRouter;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RetransmissionConnectorRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        Optional<RetransmissionOutboundConnector> emptyConnector = Optional.empty();
        Optional<CoreRetransmissionRouter> emptyRouter = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class, () -> new RetransmissionOutboundConnectorRegistrar(null, emptyRouter));
        assertThrows(NullPointerException.class,
                     () -> new RetransmissionOutboundConnectorRegistrar(emptyConnector, null));
    }

    @Test
    void givenEmpty_constructs() {
        // Given
        Optional<RetransmissionOutboundConnector> emptyConnector = Optional.empty();
        Optional<CoreRetransmissionRouter> emptyRouter = Optional.empty();

        // When, Then
        assertDoesNotThrow(() -> new RetransmissionOutboundConnectorRegistrar(emptyConnector, emptyRouter));
    }

    @Test
    void givenRegionConnector_registersAtService() {
        // Given
        Flux<RetransmissionResult> flux = Flux.empty();
        var connector = mock(RetransmissionOutboundConnector.class);
        var router = mock(CoreRetransmissionRouter.class);
        when(router.retransmissionResults()).thenReturn(flux);

        // When
        new RetransmissionOutboundConnectorRegistrar(Optional.of(connector), Optional.of(router));

        // Then
        verify(router).registerRetransmissionConnector(connector);
        verify(connector).setRetransmissionResultStream(flux);
    }
}