package energy.eddie.core.services;

import energy.eddie.api.v0_82.AccountingPointEnveloppeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnveloppe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class AccountingPointEnveloppeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointEnveloppeService.class);

    private final Sinks.Many<AccountingPointEnveloppe> accountingPointSink = Sinks.many()
                                                                                  .multicast()
                                                                                  .onBackpressureBuffer();

    public void registerProvider(AccountingPointEnveloppeProvider accountingPointEnveloppeProvider) {
        LOGGER.info("EddieAccountingPointMarketDocumentService: Registering {}",
                    accountingPointEnveloppeProvider.getClass().getName());
        accountingPointEnveloppeProvider.getAccountingPointEnveloppeFlux()
                                        .doOnNext(accountingPointSink::tryEmitNext)
                                        .doOnError(accountingPointSink::tryEmitError)
                                        .subscribe();
    }

    public Flux<AccountingPointEnveloppe> getAccountingPointEnveloppeStream() {
        return accountingPointSink.asFlux();
    }
}
