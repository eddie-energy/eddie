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
        data = stream.validatedHistoricalData()
                .map(this::toValidatedHistoricalDataMarketDocument);
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return data;
    }

    private ValidatedHistoricalDataEnvelope toValidatedHistoricalDataMarketDocument(IdentifiableValidatedHistoricalData message) {
        return null;  // TODO: Convert message to validated historical data market document
    }

    @Override
    public void close() throws Exception {
        // Nothing to do
    }
}