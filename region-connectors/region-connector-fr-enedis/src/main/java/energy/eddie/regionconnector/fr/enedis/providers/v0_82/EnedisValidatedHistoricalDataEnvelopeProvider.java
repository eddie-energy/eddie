package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("EnedisValidatedHistoricalDataEnvelopeProvider_v0_82")
public class EnedisValidatedHistoricalDataEnvelopeProvider implements ValidatedHistoricalDataEnvelopeProvider {

    private final Flux<IdentifiableMeterReading> identifiableMeterReadings;
    private final IntermediateMarketDocumentFactory intermediateMarketDocumentFactory;

    public EnedisValidatedHistoricalDataEnvelopeProvider(
            EnergyDataStreams streams,
            IntermediateMarketDocumentFactory intermediateMarketDocumentFactory
    ) {
        this.identifiableMeterReadings = streams.getValidatedHistoricalData();
        this.intermediateMarketDocumentFactory = intermediateMarketDocumentFactory;
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings
                .map(intermediateMarketDocumentFactory::create)
                .map(IntermediateValidatedHistoricalDocument::eddieValidatedHistoricalDataMarketDocument);
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
