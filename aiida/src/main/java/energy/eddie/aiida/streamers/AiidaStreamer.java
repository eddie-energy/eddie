package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.record.AiidaRecord;
import reactor.core.publisher.Flux;

public abstract class AiidaStreamer {
    protected final Flux<AiidaRecord> recordFlux;

    /**
     * Create a new AiidaStreamer and sets the recordFlux, via which any records that should be streamed are received.
     * The constructor should initialize and prepare any necessary resources but only after {@link #connect()}
     * was called, connections should be opened and data streamed.
     *
     * @param recordFlux Flux which the AiidaStreamer implementation will subscribe to and thus will receive any records that should be sent.
     */
    protected AiidaStreamer(Flux<AiidaRecord> recordFlux) {
        this.recordFlux = recordFlux;
    }

    /**
     * Open required connections to the streaming target (EP) in this method, not beforehand.
     * Also subscribe to the {@code recordFlux} in this method, to receive records that shall be sent.
     */
    public abstract void connect();

    /**
     * Close any open connections and free up any used resources so that the AiidaStreamer can be properly disposed.
     * Try to send any unsent records before closing.
     */
    public abstract void shutdown();
}
