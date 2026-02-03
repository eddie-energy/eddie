package energy.eddie.regionconnector.de.eta.providers.cim.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.de.eta.providers.AccountingPointDataStream;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Provider for accounting point data envelopes in CIM v0.82 format.
 * 
 * Aligns with EDDIE documentation:
 * - https://architecture.eddie.energy/framework/3-extending/region-connector/api.html#accountingpointenvelopeprovider
 * - Converts accounting point data from MDA to CIM v0.82 AccountingPointEnvelope
 * - Maintains backwards compatibility with CIM v0.82
 */
@Component
public class DeAccountingPointEnvelopeProvider implements AccountingPointEnvelopeProvider {

    private final Flux<IdentifiableAccountingPointData> identifiableAccountingPointData;
    private final IntermediateAccountingPointMarketDocumentFactory factory;

    public DeAccountingPointEnvelopeProvider(
            AccountingPointDataStream stream,
            IntermediateAccountingPointMarketDocumentFactory factory
    ) {
        this.identifiableAccountingPointData = stream.accountingPointData();
        this.factory = factory;
    }

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return identifiableAccountingPointData
                .map(factory::create)
                .map(IntermediateAccountingPointMarketDocument::accountingPointEnvelope);
    }

    @Override
    public void close() throws Exception {
        // No-Op - the underlying flux is managed by AccountingPointDataStream
    }
}
