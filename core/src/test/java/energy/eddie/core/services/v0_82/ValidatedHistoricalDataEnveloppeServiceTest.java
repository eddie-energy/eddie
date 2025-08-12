package energy.eddie.core.services.v0_82;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.core.converters.MeasurementConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatedHistoricalDataEnvelopeServiceTest {
    @Mock
    private MeasurementConverter converter;

    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    @Test
    void givenMultipleStreams_combinesAndEmitsAllValuesFromAllStreams() throws Exception {
        // Given
        when(converter.convert(any()))
                .thenReturn(new ValidatedHistoricalDataEnvelope());
        var service = new ValidatedHistoricalDataEnvelopeService(converter);
        Sinks.Many<ValidatedHistoricalDataEnvelope> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<ValidatedHistoricalDataEnvelope> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        ValidatedHistoricalDataEnvelopeProvider provider1 = createProvider(sink1);
        ValidatedHistoricalDataEnvelopeProvider provider2 = createProvider(sink2);

        var one = new ValidatedHistoricalDataEnvelope();
        var two = new ValidatedHistoricalDataEnvelope();
        var three = new ValidatedHistoricalDataEnvelope();

        // When
        var flux = service.getEddieValidatedHistoricalDataMarketDocumentStream();
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

    private static ValidatedHistoricalDataEnvelopeProvider createProvider(Sinks.Many<ValidatedHistoricalDataEnvelope> sink) {
        return new ValidatedHistoricalDataEnvelopeProvider() {
            @Override
            public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
                return sink.asFlux();
            }

            @Override
            public void close() {
                sink.tryEmitComplete();
            }
        };
    }

    @Test
    void givenConverter_appliesItToStream() throws Exception {
        // Given
        var service = new ValidatedHistoricalDataEnvelopeService(converter);
        when(converter.convert(any()))
                .thenReturn(new ValidatedHistoricalDataEnvelope());
        Sinks.Many<ValidatedHistoricalDataEnvelope> sink = Sinks.many().unicast().onBackpressureBuffer();

        ValidatedHistoricalDataEnvelopeProvider provider = createProvider(sink);

        var one = new ValidatedHistoricalDataEnvelope();
        // When
        var flux = service.getEddieValidatedHistoricalDataMarketDocumentStream();
        StepVerifier.create(flux)
                    .then(() -> {
                        service.registerProvider(provider);
                        sink.tryEmitNext(one);
                    })
                    // Then
                    .expectNextCount(1)
                    .thenCancel()
                    .verify();

        provider.close();
        verify(converter).convert(any());
    }
}