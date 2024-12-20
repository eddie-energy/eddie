package energy.eddie.outbound.admin.console.services;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequestRouter;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetransmissionAdminConsoleConnectorTest {
    @Mock
    private RetransmissionRequestRouter retransmissionRequestRouter;

    @Test
    void setRetransmissionResultStream_subscribesToRetransmissionRequestRouterAndClosingCleansUp() {
        // Given
        TestPublisher<RetransmissionResult> testPublisher = TestPublisher.create();
        when(retransmissionRequestRouter.retransmissionResults()).thenReturn(testPublisher.flux());

        // When
        var connector = new RetransmissionAdminConsoleOutboundConnector();
        connector.setRetransmissionResultStream(retransmissionRequestRouter.retransmissionResults());

        // Then
        testPublisher.assertSubscribers(1);
        connector.close();
        testPublisher.assertSubscribers(0);
        StepVerifier.create(connector.retransmissionRequests())
                    .verifyComplete();
    }
}
