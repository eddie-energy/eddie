package energy.eddie.core.services;

import energy.eddie.api.v0_82.EddieAccountingPointMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class EddieAccountingPointMarketDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EddieAccountingPointMarketDocumentService.class);

    private final Sinks.Many<EddieAccountingPointMarketDocument> accountingPointSink = Sinks.many()
                                                                                            .multicast()
                                                                                            .onBackpressureBuffer();

    public void registerProvider(EddieAccountingPointMarketDocumentProvider eddieAccountingPointMarketDocumentProvider) {
        LOGGER.info("EddieAccountingPointMarketDocumentService: Registering {}",
                    eddieAccountingPointMarketDocumentProvider.getClass().getName());
        eddieAccountingPointMarketDocumentProvider.getEddieAccountingPointMarketDocumentStream()
                                                  .doOnNext(accountingPointSink::tryEmitNext)
                                                  .doOnError(accountingPointSink::tryEmitError)
                                                  .subscribe();
    }

    public Flux<EddieAccountingPointMarketDocument> getEddieAccountingPointMarketDocumentStream() {
        return accountingPointSink.asFlux();
    }
}
