package energy.eddie.core.services;

import energy.eddie.api.v0_82.CimConsumptionRecordProvider;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.cim.validated_historical_data.v0_82.ValidatedHistoricalDataMarketDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.Mockito.mock;

class EddieValidatedHistoricalDataMarketDocumentServiceTest {
    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    @Test
    void givenMultipleStreams_combinesAndEmitsAllValuesFromAllStreams() {
        // Given
        var service = new EddieValidatedHistoricalDataMarketDocumentService();
        Sinks.Many<EddieValidatedHistoricalDataMarketDocument> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<EddieValidatedHistoricalDataMarketDocument> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        CimConsumptionRecordProvider provider1 = () -> JdkFlowAdapter.publisherToFlowPublisher(sink1.asFlux());
        CimConsumptionRecordProvider provider2 = () -> JdkFlowAdapter.publisherToFlowPublisher(sink2.asFlux());

        var one = new EddieValidatedHistoricalDataMarketDocument(Optional.of("one"), Optional.empty(), Optional.empty(), mock(ValidatedHistoricalDataMarketDocument.class));
        var two = new EddieValidatedHistoricalDataMarketDocument(Optional.of("two"), Optional.empty(), Optional.empty(), mock(ValidatedHistoricalDataMarketDocument.class));
        var three = new EddieValidatedHistoricalDataMarketDocument(Optional.of("three"), Optional.empty(), Optional.empty(), mock(ValidatedHistoricalDataMarketDocument.class));

        // When
        var flux = JdkFlowAdapter.flowPublisherToFlux(service.getEddieValidatedHistoricalDataMarketDocumentStream());
        StepVerifier.create(flux)
                .then(() -> {
                    service.registerProvider(provider1);
                    service.registerProvider(provider2);
                    sink2.tryEmitNext(two);
                    sink1.tryEmitNext(one);
                    sink2.tryEmitNext(three);
                })
                // Then
                .expectNextCount(3)
                .thenCancel()
                .verify();
    }
}
