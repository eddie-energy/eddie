package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnveloppeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnveloppe;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * This class is for processing incoming master data by mapping it to {@link AccountingPointEnveloppe}
 */
@Component
public class EdaAccountingPointEnveloppeProvider implements AccountingPointEnveloppeProvider {

    private final Flux<AccountingPointEnveloppe> apFlux;

    public EdaAccountingPointEnveloppeProvider(
            Flux<IdentifiableMasterData> identifiableMasterDataFlux,
            IntermediateAccountingPointMarketDocumentFactory factory
    ) {
        this.apFlux = identifiableMasterDataFlux
                .map(factory::create)
                .map(IntermediateAccountingPointMarketDocument::accountingPointEnveloppe);
    }

    @Override
    public Flux<AccountingPointEnveloppe> getAccountingPointEnveloppeFlux() {
        return apFlux;
    }

    @Override
    public void close() throws Exception {
        // Nothing to clean up, flux is closed when the underlying flux is closed
    }
}
