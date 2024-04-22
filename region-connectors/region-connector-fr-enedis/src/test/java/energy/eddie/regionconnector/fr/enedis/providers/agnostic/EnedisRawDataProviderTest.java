package energy.eddie.regionconnector.fr.enedis.providers.agnostic;

import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;

import static org.mockito.Mockito.mock;

class EnedisRawDataProviderTest {

    @Test
    void completeOnInputFlux_emitsCompleteOnRawDataFlow() {
        TestPublisher<IdentifiableMeterReading> publisher = TestPublisher.create();
        //noinspection resource StepVerifier closes provider
        var provider = new EnedisRawDataProvider(publisher.flux());

        StepVerifier.create(provider.getRawDataStream())
                .then(publisher::complete)
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void givenValueOnFlux_publishesOnFlow() {
        // Given
        TestPublisher<IdentifiableMeterReading> publisher = TestPublisher.create();
        var reading = new IdentifiableMeterReading(mock(FrEnedisPermissionRequest.class), mock(MeterReading.class));

        //noinspection resource StepVerifier closes provider
        var provider = new EnedisRawDataProvider(publisher.flux());

        StepVerifier.create(provider.getRawDataStream().log())
                // When
                .then(() -> publisher.next(reading))
                // Then
                .expectNextCount(1)
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}