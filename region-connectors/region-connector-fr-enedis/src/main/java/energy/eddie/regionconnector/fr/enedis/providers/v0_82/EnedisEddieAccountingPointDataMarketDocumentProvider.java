package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.v0_82.EddieAccountingPointMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EnedisEddieAccountingPointDataMarketDocumentProvider implements EddieAccountingPointMarketDocumentProvider {

    private final Flux<IdentifiableAccountingPointData> identifiableAccountingPointDataFlux;
    private final IntermediateMarketDocumentFactory intermediateMarketDocumentFactory;

    public EnedisEddieAccountingPointDataMarketDocumentProvider(
            Flux<IdentifiableAccountingPointData> identifiableAccountingPointDataFlux,
            IntermediateMarketDocumentFactory intermediateMarketDocumentFactory
    ) {
        this.identifiableAccountingPointDataFlux = identifiableAccountingPointDataFlux;
        this.intermediateMarketDocumentFactory = intermediateMarketDocumentFactory;
    }

    @Override
    public Flux<EddieAccountingPointMarketDocument> getEddieAccountingPointMarketDocumentStream() {
        return identifiableAccountingPointDataFlux
                .map(intermediateMarketDocumentFactory::create)
                .map(IntermediateAccountingPointDataMarketDocument::eddieAccountingPointMarketDocument);
    }


    @Override
    public void close() throws Exception {
        // No-Op
    }
}
