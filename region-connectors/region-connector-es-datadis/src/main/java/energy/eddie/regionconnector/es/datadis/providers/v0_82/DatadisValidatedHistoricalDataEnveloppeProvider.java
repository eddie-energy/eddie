package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnveloppeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnveloppe;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DatadisValidatedHistoricalDataEnveloppeProvider implements ValidatedHistoricalDataEnveloppeProvider {

    private final Flux<IdentifiableMeteringData> identifiableMeterReadings;
    private final IntermediateVHDFactory intermediateVHDFactory;

    public DatadisValidatedHistoricalDataEnveloppeProvider(
            Flux<IdentifiableMeteringData> identifiableMeterReadings,
            IntermediateVHDFactory intermediateVHDFactory
    ) {
        this.identifiableMeterReadings = identifiableMeterReadings;
        this.intermediateVHDFactory = intermediateVHDFactory;
    }

    @Override
    public Flux<ValidatedHistoricalDataEnveloppe> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings
                .map(intermediateVHDFactory::create)
                .map(IntermediateValidatedHistoricalDocument::eddieValidatedHistoricalDataMarketDocument);
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
