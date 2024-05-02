package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.v0_82.EddieAccountingPointMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * This class is for processing incoming master data by mapping it to {@link EddieAccountingPointMarketDocument}
 */
@Component
public class EdaEddieAccountingPointMarketDocumentProvider implements EddieAccountingPointMarketDocumentProvider {

    private final Flux<EddieAccountingPointMarketDocument> eddieAccountingPointMarketDocumentFlux;

    public EdaEddieAccountingPointMarketDocumentProvider(
            Flux<IdentifiableMasterData> identifiableMasterDataFlux,
            IntermediateAccountingPointMarketDocumentFactory factory
    ) {
        this.eddieAccountingPointMarketDocumentFlux = identifiableMasterDataFlux
                .map(factory::create)
                .map(IntermediateAccountingPointMarketDocument::eddieAccountingPointMarketDocument);
    }

    @Override
    public Flux<EddieAccountingPointMarketDocument> getEddieAccountingPointMarketDocumentStream() {
        return eddieAccountingPointMarketDocumentFlux;
    }

    @Override
    public void close() throws Exception {
        // Nothing to clean up, flux is closed when the underlying flux is closed
    }
}
