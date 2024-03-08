package energy.eddie.api.v0;

import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of
 * {@link ConsumptionRecord}s available.
 */
public interface Mvp1ConsumptionRecordProvider extends AutoCloseable {
    /**
     * Data stream of all consumption records received by this region connector.
     *
     * @return consumption record stream
     */
    Flux<ConsumptionRecord> getConsumptionRecordStream();

    /**
     * Emit a complete signal on the Flow in this method.
     */
    @Override
    void close() throws Exception;
}
