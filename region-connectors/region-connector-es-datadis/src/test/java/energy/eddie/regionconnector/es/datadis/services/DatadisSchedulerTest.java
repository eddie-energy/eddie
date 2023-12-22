package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.api.DataApi;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.Mockito.mock;

class DatadisSchedulerTest {
    @Test
    void close_emitsCompleteOnPublisher() {
        // Given
        var scheduler = new DatadisScheduler(mock(DataApi.class), Sinks.many().multicast().onBackpressureBuffer());

        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(scheduler.getConsumptionRecordStream()))
                .expectComplete()
                .verifyLater();

        // When
        scheduler.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }
}