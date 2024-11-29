package energy.eddie.admin.console.services;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequestRouter;
import energy.eddie.api.agnostic.retransmission.result.Success;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetransmissionAdminConsoleConnectorTest {
    @Mock
    private RetransmissionRequestRouter retransmissionRequestRouter;

    @Test
    void retransmit_callsRouteRetransmissionRequest() {
        // Given
        when(retransmissionRequestRouter.routeRetransmissionRequest(any(), any())).thenReturn(Mono.just(new Success()));
        var connector = new RetransmissionAdminConsoleConnector(retransmissionRequestRouter);
        var now = LocalDate.now(ZoneOffset.UTC);
        var request = new RetransmissionRequest("permissionId", now, now);
        var regionConnectorId = "regionConnectorId";
        // When
        connector.retransmit(regionConnectorId, request);

        // Then
        verify(retransmissionRequestRouter).routeRetransmissionRequest(regionConnectorId, request);
    }

    @Test
    void retransmitThrows_whenRetransmissionRequestRouter() {
        assertThrows(NullPointerException.class, () -> new RetransmissionAdminConsoleConnector(null));
    }
}
