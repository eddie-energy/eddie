package energy.eddie.regionconnector.es.datadis.providers.agnostic;

import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.List;

import static org.mockito.Mockito.mock;

class DatadisRawDataProviderTest {
    @Test
    void completeOnInputFlux_emitsCompleteOnRawDataFlow() {
        TestPublisher<IdentifiableMeteringData> publisher = TestPublisher.create();
        //noinspection resource StepVerifier closes provider
        var provider = new DatadisRawDataProvider(publisher.flux());

        StepVerifier.create(provider.getRawDataStream())
                .then(publisher::complete)
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void givenValueOnFlux_publishesOnFlow() {
        // Given
        TestPublisher<IdentifiableMeteringData> publisher = TestPublisher.create();
        var reading = new IdentifiableMeteringData(mock(EsPermissionRequest.class), List.of(mock(MeteringData.class)));

        //noinspection resource StepVerifier closes provider
        var provider = new DatadisRawDataProvider(publisher.flux());

        StepVerifier.create(provider.getRawDataStream())
                // When
                .then(() -> publisher.next(reading))
                // Then
                .expectNextCount(1)
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}