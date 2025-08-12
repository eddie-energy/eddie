package energy.eddie.outbound.rest.connectors;

import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.cim.v0_91_08.ESMPDateTimeInterval;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestRetransmissionConnectorTest {
    @Test
    void publish_returnsResult() {
        // Given
        var doc = new RTREnvelope()
                .withMarketDocumentMRID("pid")
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMarketDocumentPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart("2025-01-01T00:00Z")
                                .withEnd("2025-01-09T00:00Z")
                );
        var success = new Success("pid", ZonedDateTime.now(ZoneOffset.UTC));
        var testPublisher = TestPublisher.<RetransmissionResult>create();
        var connector = new RestRetransmissionConnector();
        connector.setRetransmissionResultStream(testPublisher.flux());

        // When
        var res = connector.publish(doc);
        testPublisher.emit(success);

        // Then
        StepVerifier.create(res)
                    .assertNext(result -> assertEquals(success, result))
                    .verifyComplete();
        connector.close();
    }
}