package energy.eddie.core.services;

import energy.eddie.api.v0_82.EddieAccountingPointMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import energy.eddie.cim.v0_82.ap.AccountingPointMarketDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.Mockito.mock;

class EddieAccountingPointMarketDocumentServiceTest {
    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    @Test
    void givenMultipleStreams_combinesAndEmitsAllValuesFromAllStreams() throws Exception {
        // Given
        var service = new EddieAccountingPointMarketDocumentService();
        Sinks.Many<EddieAccountingPointMarketDocument> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<EddieAccountingPointMarketDocument> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        EddieAccountingPointMarketDocumentProvider provider1 = createProvider(sink1);
        EddieAccountingPointMarketDocumentProvider provider2 = createProvider(sink2);

        var one = new EddieAccountingPointMarketDocument(Optional.of("one"), Optional.empty(), Optional.empty(), mock(
                AccountingPointMarketDocument.class));
        var two = new EddieAccountingPointMarketDocument(Optional.of("two"),
                                                         Optional.empty(),
                                                         Optional.empty(),
                                                         mock(AccountingPointMarketDocument.class));
        var three = new EddieAccountingPointMarketDocument(Optional.of("three"),
                                                           Optional.empty(),
                                                           Optional.empty(),
                                                           mock(AccountingPointMarketDocument.class));

        // When
        var flux = service.getEddieAccountingPointMarketDocumentStream();
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

    private static EddieAccountingPointMarketDocumentProvider createProvider(Sinks.Many<EddieAccountingPointMarketDocument> sink) {
        return new EddieAccountingPointMarketDocumentProvider() {
            @Override
            public Flux<EddieAccountingPointMarketDocument> getEddieAccountingPointMarketDocumentStream() {
                return sink.asFlux();
            }

            @Override
            public void close() {
                sink.tryEmitComplete();
            }
        };
    }
}
