package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * This class is for processing incoming master data by mapping it to {@link AccountingPointEnvelope}
 */
@Component
public class EdaAccountingPointEnvelopeProvider implements AccountingPointEnvelopeProvider {

    private final Flux<AccountingPointEnvelope> apFlux;

    public EdaAccountingPointEnvelopeProvider(
            Flux<IdentifiableMasterData> identifiableMasterDataFlux,
            IntermediateAccountingPointMarketDocumentFactory factory
    ) {
        this.apFlux = identifiableMasterDataFlux
                .map(factory::create)
                .map(IntermediateAccountingPointMarketDocument::accountingPointEnvelope);
    }

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return apFlux;
    }

    @Override
    public void close() throws Exception {
        // Nothing to clean up, flux is closed when the underlying flux is closed
    }
}
