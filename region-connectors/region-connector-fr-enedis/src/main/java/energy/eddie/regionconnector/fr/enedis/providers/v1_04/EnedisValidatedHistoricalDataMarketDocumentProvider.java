package energy.eddie.regionconnector.fr.enedis.providers.v1_04;

import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("EnedisValidatedHistoricalDataMarketDocumentProvider_v1_04")
public class EnedisValidatedHistoricalDataMarketDocumentProvider implements ValidatedHistoricalDataMarketDocumentProvider {

    private final Flux<IdentifiableMeterReading> identifiableMeterReadings;
    private final EnedisConfiguration enedisConfig;

    public EnedisValidatedHistoricalDataMarketDocumentProvider(
            EnergyDataStreams streams,
            EnedisConfiguration enedisConfig
    ) {
        this.identifiableMeterReadings = streams.getValidatedHistoricalData();
        this.enedisConfig = enedisConfig;
    }

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings
                .map(id -> new IntermediateValidatedHistoricalDocument(id, enedisConfig))
                .map(IntermediateValidatedHistoricalDocument::value);
    }
}
