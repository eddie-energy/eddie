package energy.eddie.spring.regionconnector.extensions.cim.v0_82.cmd;

import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

class CommonConsentMarketDocumentProviderTest {
    @Test
    void testGetConsentMarketDocumentStream_publishesElements() {
        // Given
        Sinks.Many<ConsentMarketDocument> cmdSink = Sinks.many().multicast().onBackpressureBuffer();
        @SuppressWarnings("resource") // The try-with resource is closed in the step verifier
        var cmdProvider = new CommonConsentMarketDocumentProvider(cmdSink);

        // When
        StepVerifier.create(cmdProvider.getConsentMarketDocumentStream())
                .then(() -> {
                    cmdSink.tryEmitNext(new ConsentMarketDocument());
                    cmdProvider.close();
                })
                // Then
                .expectNextCount(1)
                .verifyComplete();
    }

}