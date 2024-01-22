package energy.eddie.regionconnector.dk.energinet.providers.v0;

import energy.eddie.api.v0.ConnectionStatusMessage;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

class EnerginetMvp1ConnectionStatusMessageProviderTest {
    @Test
    void close_emitsCompleteOnPublisher() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        var connectionStatusMessageProvider = new EnerginetMvp1ConnectionStatusMessageProvider(permissionStateMessages);
        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(connectionStatusMessageProvider.getConnectionStatusMessageStream()))
                .expectComplete()
                .verifyLater();

        // When
        connectionStatusMessageProvider.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

}