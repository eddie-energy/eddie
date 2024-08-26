package energy.eddie.core.services;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnveloppeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnveloppe;
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
class ValidatedHistoricalDataEnveloppeServiceTest {
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
                .thenReturn(new ValidatedHistoricalDataEnveloppe());
        var service = new ValidatedHistoricalDataEnveloppeService(converter);
        Sinks.Many<ValidatedHistoricalDataEnveloppe> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<ValidatedHistoricalDataEnveloppe> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        ValidatedHistoricalDataEnveloppeProvider provider1 = createProvider(sink1);
        ValidatedHistoricalDataEnveloppeProvider provider2 = createProvider(sink2);

        var one = new ValidatedHistoricalDataEnveloppe();
        var two = new ValidatedHistoricalDataEnveloppe();
        var three = new ValidatedHistoricalDataEnveloppe();

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

    private static ValidatedHistoricalDataEnveloppeProvider createProvider(Sinks.Many<ValidatedHistoricalDataEnveloppe> sink) {
        return new ValidatedHistoricalDataEnveloppeProvider() {
            @Override
            public Flux<ValidatedHistoricalDataEnveloppe> getValidatedHistoricalDataMarketDocumentsStream() {
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
        var service = new ValidatedHistoricalDataEnveloppeService(converter);
        when(converter.convert(any()))
                .thenReturn(new ValidatedHistoricalDataEnveloppe());
        Sinks.Many<ValidatedHistoricalDataEnveloppe> sink = Sinks.many().unicast().onBackpressureBuffer();

        ValidatedHistoricalDataEnveloppeProvider provider = createProvider(sink);

        var one = new ValidatedHistoricalDataEnveloppe();
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