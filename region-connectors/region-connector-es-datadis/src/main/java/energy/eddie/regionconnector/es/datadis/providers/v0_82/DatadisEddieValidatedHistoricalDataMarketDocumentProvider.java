package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.EddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DatadisEddieValidatedHistoricalDataMarketDocumentProvider implements EddieValidatedHistoricalDataMarketDocumentProvider {

    private final Flux<IdentifiableMeteringData> identifiableMeterReadings;
    private final IntermediateVHDFactory intermediateVHDFactory;

    public DatadisEddieValidatedHistoricalDataMarketDocumentProvider(
            Flux<IdentifiableMeteringData> identifiableMeterReadings,
            IntermediateVHDFactory intermediateVHDFactory) {
        this.identifiableMeterReadings = identifiableMeterReadings;
        this.intermediateVHDFactory = intermediateVHDFactory;
    }

    @Override
    public Flux<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return identifiableMeterReadings
                .map(intermediateVHDFactory::create)
                .map(IntermediateValidatedHistoricalDocument::eddieValidatedHistoricalDataMarketDocument);
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
