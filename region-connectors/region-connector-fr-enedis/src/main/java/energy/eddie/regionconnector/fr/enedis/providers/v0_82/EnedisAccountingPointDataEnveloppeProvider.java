package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnveloppeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnveloppe;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EnedisAccountingPointDataEnveloppeProvider implements AccountingPointEnveloppeProvider {

    private final Flux<IdentifiableAccountingPointData> identifiableAccountingPointDataFlux;
    private final IntermediateMarketDocumentFactory intermediateMarketDocumentFactory;

    public EnedisAccountingPointDataEnveloppeProvider(
            Flux<IdentifiableAccountingPointData> identifiableAccountingPointDataFlux,
            IntermediateMarketDocumentFactory intermediateMarketDocumentFactory
    ) {
        this.identifiableAccountingPointDataFlux = identifiableAccountingPointDataFlux;
        this.intermediateMarketDocumentFactory = intermediateMarketDocumentFactory;
    }

    @Override
    public Flux<AccountingPointEnveloppe> getAccountingPointEnveloppeFlux() {
        return identifiableAccountingPointDataFlux
                .map(intermediateMarketDocumentFactory::create)
                .map(IntermediateAccountingPointDataMarketDocument::accountingPointEnveloppe);
    }


    @Override
    public void close() throws Exception {
        // No-Op
    }
}
