package energy.eddie.regionconnector.cds.providers.ap;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class CdsAccountingPointDataMarketDocumentProvider implements AccountingPointEnvelopeProvider {
    private final IdentifiableDataStreams streams;

    public CdsAccountingPointDataMarketDocumentProvider(IdentifiableDataStreams streams) {this.streams = streams;}

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return streams.accountingPointData()
                      .map(IntermediateAccountingPointDocument::new)
                      .flatMapIterable(IntermediateAccountingPointDocument::toAp);
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
