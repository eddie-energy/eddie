package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

class NlPermissionMarketDocumentProviderTest {

    @SuppressWarnings("resource")
    @Test
    void testGetPermissionMarketDocumentStream() {
        // Given
        var pmdProvider = new NlPermissionMarketDocumentProvider(Sinks.many().multicast().onBackpressureBuffer());

        // When
        var res = pmdProvider.getPermissionMarketDocumentStream();
        // Then
        StepVerifier.create(res)
                    .then(pmdProvider::close)
                    .verifyComplete();
    }
}