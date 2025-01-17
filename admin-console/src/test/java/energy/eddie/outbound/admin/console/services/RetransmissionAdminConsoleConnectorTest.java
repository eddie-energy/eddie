package energy.eddie.outbound.admin.console.services;

import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

@ExtendWith(MockitoExtension.class)
class RetransmissionAdminConsoleConnectorTest {
    @Test
    void setRetransmissionResultStream_subscribesToRetransmissionRequestRouterAndClosingCleansUp() {
        // Given
        TestPublisher<RetransmissionResult> testPublisher = TestPublisher.create();

        // When
        var connector = new RetransmissionAdminConsoleOutboundConnector();
        connector.setRetransmissionResultStream(testPublisher.flux());

        // Then
        testPublisher.assertSubscribers(1);
        connector.close();
        testPublisher.assertSubscribers(0);
        StepVerifier.create(connector.retransmissionRequests())
                    .verifyComplete();
    }
}
