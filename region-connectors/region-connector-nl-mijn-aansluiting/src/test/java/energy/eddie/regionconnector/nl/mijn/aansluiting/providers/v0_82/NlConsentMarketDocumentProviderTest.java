package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

class NlConsentMarketDocumentProviderTest {

    @SuppressWarnings("resource")
    @Test
    void testGetConsentMarketDocumentStream() {
        // Given
        var cmdProvider = new NlConsentMarketDocumentProvider(Sinks.many().multicast().onBackpressureBuffer());

        // When
        var res = cmdProvider.getConsentMarketDocumentStream();
        // Then
        StepVerifier.create(res)
                    .then(cmdProvider::close)
                    .verifyComplete();
    }
}