package energy.eddie.regionconnector.de.eta.providers.cim.v104;

import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Provides CIM v1.04 Validated Historical Data Market Documents for the German (DE) region connector.
 * This class streams validated historical metering data in CIM format to outbound connectors.
 * 
 * Note: This is registered as a bean via the Spring config, not as a @Component.
 */
public class DeValidatedHistoricalDataMarketDocumentProvider implements ValidatedHistoricalDataMarketDocumentProvider {
    
    private final Sinks.Many<VHDEnvelope> documentSink;

    public DeValidatedHistoricalDataMarketDocumentProvider() {
        this.documentSink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return documentSink.asFlux();
    }

    /**
     * Emit a validated historical data market document to all subscribers
     * 
     * @param document the CIM document envelope
     */
    public void emitDocument(VHDEnvelope document) {
        documentSink.tryEmitNext(document);
    }
}
