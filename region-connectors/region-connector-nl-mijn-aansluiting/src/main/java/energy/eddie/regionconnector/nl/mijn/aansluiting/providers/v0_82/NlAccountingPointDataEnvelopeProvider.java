package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class NlAccountingPointDataEnvelopeProvider implements AccountingPointEnvelopeProvider {
    private final Flux<AccountingPointEnvelope> flux;

    public NlAccountingPointDataEnvelopeProvider(
            PollingService pollingService,
            MijnAansluitingConfiguration config
    ) {
        flux = pollingService.identifiableAccountingPointDataFlux()
                             .map(res -> new IntermediateAccountingPointDataMarketDocument(res, config))
                             .flatMapIterable(IntermediateAccountingPointDataMarketDocument::toAp);
    }

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return flux;
    }

    @Override
    public void close() {
        // No Op
    }
}
