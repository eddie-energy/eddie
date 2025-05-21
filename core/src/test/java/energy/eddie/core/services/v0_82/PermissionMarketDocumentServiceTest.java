package energy.eddie.core.services.v0_82;

import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

class PermissionMarketDocumentServiceTest {
    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    @Test
    void givenMultipleStreams_combinesAndEmitsAllValuesFromAllStreams() throws Exception {
        // Given
        var service = new PermissionMarketDocumentService();
        Sinks.Many<PermissionEnvelope> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<PermissionEnvelope> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        PermissionMarketDocumentProvider provider1 = createProvider(sink1);
        PermissionMarketDocumentProvider provider2 = createProvider(sink2);

        var one = new PermissionEnvelope();
        var two = new PermissionEnvelope();
        var three = new PermissionEnvelope();

        // When
        var flux = service.getPermissionMarketDocumentStream();
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

    private static PermissionMarketDocumentProvider createProvider(Sinks.Many<PermissionEnvelope> sink) {
        return new PermissionMarketDocumentProvider() {
            @Override
            public Flux<PermissionEnvelope> getPermissionMarketDocumentStream() {
                return sink.asFlux();
            }

            @Override
            public void close() {
                sink.tryEmitComplete();
            }
        };
    }
}