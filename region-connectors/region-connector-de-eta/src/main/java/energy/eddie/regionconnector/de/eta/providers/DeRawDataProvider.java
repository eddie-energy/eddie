package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Provides raw data messages from the German (DE) MDA.
 * This class streams raw, unprocessed data received from ETA Plus to outbound connectors.
 * 
 * Note: This is registered as a bean via the Spring config, not as a @Component.
 */
public class DeRawDataProvider implements RawDataProvider {
    
    private final Sinks.Many<RawDataMessage> rawDataSink;

    public DeRawDataProvider() {
        this.rawDataSink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return rawDataSink.asFlux();
    }

    @Override
    public void close() {
        rawDataSink.tryEmitComplete();
    }

    /**
     * Emit a raw data message to all subscribers
     * 
     * @param message the raw data message to emit
     */
    public void emitRawData(RawDataMessage message) {
        rawDataSink.tryEmitNext(message);
    }
}
