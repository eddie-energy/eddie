package energy.eddie.core.services;

import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

class ConsentMarketDocumentServiceTest {
    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    private static ConsentMarketDocumentProvider createProvider(Sinks.Many<ConsentMarketDocument> sink) {
        return new ConsentMarketDocumentProvider() {
            @Override
            public Flux<ConsentMarketDocument> getConsentMarketDocumentStream() {
                return sink.asFlux();
            }

            @Override
            public void close() {
                sink.tryEmitComplete();
            }
        };
    }

    @Test
    void givenMultipleStreams_combinesAndEmitsAllValuesFromAllStreams() throws Exception {
        // Given
        var service = new ConsentMarketDocumentService();
        Sinks.Many<ConsentMarketDocument> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<ConsentMarketDocument> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        ConsentMarketDocumentProvider provider1 = createProvider(sink1);
        ConsentMarketDocumentProvider provider2 = createProvider(sink2);

        var one = new ConsentMarketDocument();
        var two = new ConsentMarketDocument();
        var three = new ConsentMarketDocument();

        // When
        var flux = service.getConsentMarketDocumentStream();
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

        provider1.close();
        provider2.close();
    }
}