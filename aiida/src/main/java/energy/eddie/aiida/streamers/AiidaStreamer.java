package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.record.AiidaRecord;
import reactor.core.publisher.Flux;

public abstract class AiidaStreamer implements AutoCloseable {
    protected final Flux<AiidaRecord> recordFlux;
    protected final Flux<ConnectionStatusMessage> statusMessageFlux;

    /**
     * Create a new AiidaStreamer and sets the Flux for records and status messages that should be sent.
     * The constructor should initialize and prepare any necessary resources but only after {@link #connect()}
     * was called, connections should be opened and data streamed.
     *
     * @param recordFlux Flux which the AiidaStreamer implementation will subscribe to and thus will receive any records that should be sent.
     * @param statusMessageFlux Flux on which ConnectionStatusMessage that should be sent are available.
     */
    protected AiidaStreamer(Flux<AiidaRecord> recordFlux, Flux<ConnectionStatusMessage> statusMessageFlux) {
        this.recordFlux = recordFlux;
        this.statusMessageFlux = statusMessageFlux;
    }

    /**
     * Open required connections to the streaming target (EP) in this method, not beforehand.
     * Also subscribe to the {@code recordFlux} in this method, to receive records that shall be sent.
     */
    public abstract void connect();
}
