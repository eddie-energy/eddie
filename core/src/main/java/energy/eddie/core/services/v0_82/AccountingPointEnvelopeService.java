package energy.eddie.core.services.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class AccountingPointEnvelopeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointEnvelopeService.class);

    private final Sinks.Many<AccountingPointEnvelope> accountingPointSink = Sinks.many()
                                                                                  .multicast()
                                                                                  .onBackpressureBuffer();

    public void registerProvider(AccountingPointEnvelopeProvider accountingPointEnvelopeProvider) {
        LOGGER.info("EddieAccountingPointMarketDocumentService: Registering {}",
                    accountingPointEnvelopeProvider.getClass().getName());
        accountingPointEnvelopeProvider.getAccountingPointEnvelopeFlux()
                                        .doOnNext(accountingPointSink::tryEmitNext)
                                        .doOnError(accountingPointSink::tryEmitError)
                                        .subscribe();
    }

    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeStream() {
        return accountingPointSink.asFlux();
    }
}
