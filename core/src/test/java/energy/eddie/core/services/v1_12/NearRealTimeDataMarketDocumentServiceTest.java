package energy.eddie.core.services.v1_12;

import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

@ExtendWith(MockitoExtension.class)
class NearRealTimeDataMarketDocumentServiceTest {
    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    @Test
    void givenMultipleStreams_combinesAndEmitsAllValuesFromAllStreams() {
        // Given
        var service = new energy.eddie.core.services.v1_12.NearRealTimeDataMarketDocumentService();
        Sinks.Many<RTDEnvelope> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<RTDEnvelope> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        service.registerProvider(sink1::asFlux);
        service.registerProvider(sink2::asFlux);

        var one = new RTDEnvelope();
        var two = new RTDEnvelope();
        var three = new RTDEnvelope();

        // When
        var flux = service.getNearRealTimeDataMarketDocumentStream();
        StepVerifier.create(flux)
                    .then(() -> {
                        sink2.tryEmitNext(two);
                        sink1.tryEmitNext(one);
                        sink2.tryEmitNext(three);
                    })
                    // Then
                    .expectNextCount(3)
                    .thenCancel()
                    .verify();
    }

    @Test
    void givenConverter_appliesItToStream() {
        // Given
        var service = new NearRealTimeDataMarketDocumentService();
        Sinks.Many<RTDEnvelope> sink = Sinks.many().unicast().onBackpressureBuffer();

        service.registerProvider(sink::asFlux);

        var one = new RTDEnvelope();
        // When
        var flux = service.getNearRealTimeDataMarketDocumentStream();
        StepVerifier.create(flux)
                    .then(() -> sink.tryEmitNext(one))
                    // Then
                    .expectNextCount(1)
                    .thenCancel()
                    .verify();
    }
}