package energy.eddie.core.services;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

class ConsumptionRecordServiceTest {
    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    private static Mvp1ConsumptionRecordProvider createProvider(Sinks.Many<ConsumptionRecord> sink) {
        return new Mvp1ConsumptionRecordProvider() {
            @Override
            public Flux<ConsumptionRecord> getConsumptionRecordStream() {
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
        ConsumptionRecordService consumptionRecordService = new ConsumptionRecordService();
        Sinks.Many<ConsumptionRecord> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<ConsumptionRecord> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        Mvp1ConsumptionRecordProvider provider1 = createProvider(sink1);
        Mvp1ConsumptionRecordProvider provider2 = createProvider(sink2);

        // When
        var flux = consumptionRecordService.getConsumptionRecordStream();
        StepVerifier.create(flux)
                .then(() -> {
                    consumptionRecordService.registerProvider(provider1);
                    sink1.tryEmitNext(new ConsumptionRecord().withConnectionId("one"));
                    sink1.tryEmitNext(new ConsumptionRecord().withConnectionId("two"));
                })
                // Then
                .expectNextCount(2)
                // When
                .then(() -> {
                    consumptionRecordService.registerProvider(provider2);
                    sink1.tryEmitNext(new ConsumptionRecord().withConnectionId("three"));
                    sink2.tryEmitNext(new ConsumptionRecord().withConnectionId("four"));
                })
                // Then
                .expectNextCount(2)
                .thenCancel()
                .verify();

        provider1.close();
        provider2.close();
    }
}
