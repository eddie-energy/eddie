package energy.eddie.spring.regionconnector.extensions.cim.v0_82.pmd;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

class CommonPermissionMarketDocumentProviderTest {
    @Test
    void testGetPermissionMarketDocumentStream_publishesElements() {
        // Given
        Sinks.Many<PermissionEnvelope> pmdSink = Sinks.many().multicast().onBackpressureBuffer();
        @SuppressWarnings("resource") // The try-with resource is closed in the step verifier
        var pmdProvider = new CommonPermissionMarketDocumentProvider(pmdSink);

        // When
        StepVerifier.create(pmdProvider.getPermissionMarketDocumentStream())
                    .then(() -> {
                        pmdSink.tryEmitNext(new PermissionEnvelope());
                        pmdProvider.close();
                    })
                    // Then
                    .expectNextCount(1)
                    .verifyComplete();
    }
}