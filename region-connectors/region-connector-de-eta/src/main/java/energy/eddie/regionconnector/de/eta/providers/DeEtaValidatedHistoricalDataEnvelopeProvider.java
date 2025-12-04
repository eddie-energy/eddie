package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.de.eta.providers.vhd.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.streams.ValidatedHistoricalDataStream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DeEtaValidatedHistoricalDataEnvelopeProvider implements ValidatedHistoricalDataEnvelopeProvider {

    private final Flux<ValidatedHistoricalDataEnvelope> data;

    public DeEtaValidatedHistoricalDataEnvelopeProvider(ValidatedHistoricalDataStream stream) {
        this.data = stream.validatedHistoricalData()
                .map(this::toValidatedHistoricalDataEnvelope);
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return data;
    }


    private ValidatedHistoricalDataEnvelope toValidatedHistoricalDataEnvelope(IdentifiableValidatedHistoricalData message) {
        return null; // TODO: Echtes Mapping implementieren

    }

    @Override
    public void close() throws Exception {
        // Nothing to do
    }
}