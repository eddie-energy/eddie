package energy.eddie.aiida.datasources;

import energy.eddie.aiida.models.record.AiidaRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public abstract class AiidaDataSource implements AutoCloseable {
    protected Sinks.Many<AiidaRecord> recordSink;
    private final String name;

    /**
     * Creates a new {@code AiidaDataSource} with the specified display name.
     *
     * @param name Display name of this new datasource.
     */
    protected AiidaDataSource(String name) {
        this.name = name;
        recordSink = Sinks.many().unicast().onBackpressureBuffer();
    }

    /**
     * Starts this datasource and all records will be published on the returned Flux.
     *
     * @return Flux on which all data from this datasource will be published.
     */
    public abstract Flux<AiidaRecord> start();

    /**
     * Closes any open connections and frees resources used by this datasource.
     * Also emits a complete signal on the Flux returned by {@link #start()}.
     */
    @Override
    public abstract void close();

    /**
     * Returns the display name for this datasource.
     *
     * @return Display name of this datasource.
     */
    public String name() {
        return name;
    }
}
