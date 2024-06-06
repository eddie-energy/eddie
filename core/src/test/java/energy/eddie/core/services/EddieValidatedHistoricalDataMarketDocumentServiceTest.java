package energy.eddie.core.services;

import energy.eddie.api.v0_82.EddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocument;
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
class EddieValidatedHistoricalDataMarketDocumentServiceTest {
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
                .thenReturn(new ValidatedHistoricalDataMarketDocument());
        var service = new EddieValidatedHistoricalDataMarketDocumentService(converter);
        Sinks.Many<EddieValidatedHistoricalDataMarketDocument> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<EddieValidatedHistoricalDataMarketDocument> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        EddieValidatedHistoricalDataMarketDocumentProvider provider1 = createProvider(sink1);
        EddieValidatedHistoricalDataMarketDocumentProvider provider2 = createProvider(sink2);

        var one = new EddieValidatedHistoricalDataMarketDocument("one",
                                                                 "one",
                                                                 "one",
                                                                 new ValidatedHistoricalDataMarketDocument());
        var two = new EddieValidatedHistoricalDataMarketDocument("two",
                                                                 "two",
                                                                 "two",
                                                                 new ValidatedHistoricalDataMarketDocument());
        var three = new EddieValidatedHistoricalDataMarketDocument("three",
                                                                   "three",
                                                                   "three",
                                                                   new ValidatedHistoricalDataMarketDocument());
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

    private static EddieValidatedHistoricalDataMarketDocumentProvider createProvider(Sinks.Many<EddieValidatedHistoricalDataMarketDocument> sink) {
        return new EddieValidatedHistoricalDataMarketDocumentProvider() {
            @Override
            public Flux<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
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
        var service = new EddieValidatedHistoricalDataMarketDocumentService(converter);
        when(converter.convert(any()))
                .thenReturn(new ValidatedHistoricalDataMarketDocument());
        Sinks.Many<EddieValidatedHistoricalDataMarketDocument> sink = Sinks.many().unicast().onBackpressureBuffer();

        EddieValidatedHistoricalDataMarketDocumentProvider provider = createProvider(sink);

        var one = new EddieValidatedHistoricalDataMarketDocument("one",
                                                                 "one",
                                                                 "one",
                                                                 new ValidatedHistoricalDataMarketDocument());
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