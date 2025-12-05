package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DatadisValidatedHistoricalDataEnvelopeProvider implements ValidatedHistoricalDataEnvelopeProvider {
    private final Flux<IdentifiableMeteringData> identifiableMeterReadings;
    private final IntermediateVHDFactory intermediateVHDFactory;

    public DatadisValidatedHistoricalDataEnvelopeProvider(
            EnergyDataStreams streams,
            IntermediateVHDFactory intermediateVHDFactory
    ) {
        this.identifiableMeterReadings = streams.getValidatedHistoricalData();
        this.intermediateVHDFactory = intermediateVHDFactory;
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings
                .map(intermediateVHDFactory::create)
                .map(IntermediateValidatedHistoricalDocument::eddieValidatedHistoricalDataMarketDocument);
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
