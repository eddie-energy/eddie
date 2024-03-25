package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.record.AiidaRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public abstract class AiidaStreamer implements AutoCloseable {
    protected final Flux<AiidaRecord> recordFlux;
    protected final Flux<ConnectionStatusMessage> statusMessageFlux;
    protected final Sinks.One<String> terminationRequestSink;

    /**
     * Create a new AiidaStreamer and sets the Flux for records and status messages that should be sent.
     * The constructor should initialize and prepare any necessary resources but only after {@link #connect()}
     * was called, connections should be opened and data streamed.
     *
     * @param recordFlux             Flux, where records that should be sent are available.
     * @param statusMessageFlux      Flux, where status messages that should be sent are available.
     * @param terminationRequestSink Sink, to which the permissionId will be published, when the EP requests a termination.
     */
    protected AiidaStreamer(Flux<AiidaRecord> recordFlux, Flux<ConnectionStatusMessage> statusMessageFlux,
                            Sinks.One<String> terminationRequestSink) {
        this.recordFlux = recordFlux;
        this.statusMessageFlux = statusMessageFlux;
        this.terminationRequestSink = terminationRequestSink;
    }

    /**
     * Open required connections to the streaming target (EP) in this method, not beforehand.
     * Subscribe to the Fluxes in this method, to receive records and status messages that shall be sent.
     * Start listening for termination requests from the EP.
     */
    public abstract void connect();

    /**
     * Unsubscribe from any Flux and free any used resources in this method.
     * May flush all queued messages beforehand.
     */
    @Override
    public abstract void close();
}
