package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.EddieAccountingPointMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DatadisEddieAccountingPointMarketDocumentProvider implements EddieAccountingPointMarketDocumentProvider {

    private final Flux<IdentifiableAccountingPointData> identifiableMeterReadings;
    private final IntermediateAPMDFactory intermediateAPMDFactory;

    public DatadisEddieAccountingPointMarketDocumentProvider(
            Flux<IdentifiableAccountingPointData> identifiableMeterReadings,
            IntermediateAPMDFactory intermediateAPMDFactory
    ) {
        this.identifiableMeterReadings = identifiableMeterReadings;
        this.intermediateAPMDFactory = intermediateAPMDFactory;
    }

    @Override
    public Flux<EddieAccountingPointMarketDocument> getEddieAccountingPointMarketDocumentStream() {
        return identifiableMeterReadings
                .map(intermediateAPMDFactory::create)
                .map(IntermediateAccountingPointMarketDocument::eddieAccountingPointMarketDocument);
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
