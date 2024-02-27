package energy.eddie.regionconnector.fr.enedis.providers.agnostic;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class EnedisRawDataProviderTest {
    @Mock
    private PermissionRequestRepository<FrEnedisPermissionRequest> mockRepo;

    @Test
    void completeOnInputFlux_emitsCompleteOnRawDataFlow() {
        TestPublisher<IdentifiableMeterReading> publisher = TestPublisher.create();
        //noinspection resource StepVerifier closes provider
        var provider = new EnedisRawDataProvider(publisher.flux(), mockRepo);

        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(provider.getRawDataStream()))
                .then(publisher::complete)
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void givenValueOnFlux_publishesOnFlow() {
        // Given
        TestPublisher<IdentifiableMeterReading> publisher = TestPublisher.create();
        var reading = new IdentifiableMeterReading("foo", "bar", "dId", mock(ConsumptionLoadCurveMeterReading.class));

        //noinspection resource StepVerifier closes provider
        var provider = new EnedisRawDataProvider(publisher.flux(), mockRepo);

        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(provider.getRawDataStream()).log())
                // When
                .then(() -> publisher.next(reading))
                // Then
                .expectNextCount(1)
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}