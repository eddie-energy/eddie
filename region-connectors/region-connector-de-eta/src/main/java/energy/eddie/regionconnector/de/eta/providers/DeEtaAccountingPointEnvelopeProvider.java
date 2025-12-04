package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.de.eta.providers.apd.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.de.eta.streams.AccountingPointDataStream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DeEtaAccountingPointEnvelopeProvider implements AccountingPointEnvelopeProvider {

    private final Flux<AccountingPointEnvelope> flux;

    public DeEtaAccountingPointEnvelopeProvider(AccountingPointDataStream stream) {
        this.flux = stream.accountingPointData()
                .map(this::toEnvelope);
    }

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return flux;
    }

    private AccountingPointEnvelope toEnvelope(IdentifiableAccountingPointData message) {
        // TODO: Implement real Mapping later.
        return null;
    }

    @Override
    public void close() throws Exception {
        // Does nothing
    }
}