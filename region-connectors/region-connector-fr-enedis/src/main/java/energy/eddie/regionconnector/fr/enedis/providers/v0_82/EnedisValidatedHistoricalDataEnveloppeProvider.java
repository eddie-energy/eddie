package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnveloppeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnveloppe;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EnedisValidatedHistoricalDataEnveloppeProvider implements ValidatedHistoricalDataEnveloppeProvider {

    private final Flux<IdentifiableMeterReading> identifiableMeterReadings;
    private final IntermediateMarketDocumentFactory intermediateMarketDocumentFactory;

    public EnedisValidatedHistoricalDataEnveloppeProvider(
            Flux<IdentifiableMeterReading> identifiableMeterReadings,
            IntermediateMarketDocumentFactory intermediateMarketDocumentFactory
    ) {
        this.identifiableMeterReadings = identifiableMeterReadings;
        this.intermediateMarketDocumentFactory = intermediateMarketDocumentFactory;
    }

    @Override
    public Flux<ValidatedHistoricalDataEnveloppe> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings
                .map(intermediateMarketDocumentFactory::create)
                .map(IntermediateValidatedHistoricalDocument::eddieValidatedHistoricalDataMarketDocument);
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
