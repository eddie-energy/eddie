package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.cds.providers.cim.CimStruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class CdsValidatedHistoricalDataMarketDocumentProvider implements ValidatedHistoricalDataEnvelopeProvider {
    private final Flux<ValidatedHistoricalDataEnvelope> validatedHistoricalDataEnvelopeFlux;

    public CdsValidatedHistoricalDataMarketDocumentProvider(IdentifiableDataStreams streams) {
        validatedHistoricalDataEnvelopeFlux = streams.validatedHistoricalData()
                                                     .map(res -> new IntermediateValidatedHistoricalDataMarketDocument(
                                                             res.permissionRequest(),
                                                             new CimStruct(res).get()
                                                     ))
                                                     .flatMapIterable(IntermediateValidatedHistoricalDataMarketDocument::toVhds);
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalDataEnvelopeFlux;
    }

    @Override
    public void close() {
        // No Op
    }
}
