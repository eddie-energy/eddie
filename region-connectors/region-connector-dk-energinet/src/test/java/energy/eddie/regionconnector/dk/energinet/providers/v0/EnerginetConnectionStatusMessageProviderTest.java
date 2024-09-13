package energy.eddie.regionconnector.dk.energinet.providers.v0;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

class EnerginetConnectionStatusMessageProviderTest {
    @Test
    void close_emitsCompleteOnPublisher() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        var connectionStatusMessageProvider = new EnerginetConnectionStatusMessageProvider(permissionStateMessages);
        StepVerifier stepVerifier = StepVerifier.create(
                        connectionStatusMessageProvider.getConnectionStatusMessageStream())
                .expectComplete()
                .verifyLater();

        // When
        connectionStatusMessageProvider.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

}
