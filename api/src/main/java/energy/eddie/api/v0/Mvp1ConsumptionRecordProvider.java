package energy.eddie.api.v0;

import java.util.concurrent.Flow;

/**
 * Used to extend a {@link RegionConnector} by making a Flow.Publisher of
 * {@link ConsumptionRecord}s available.
 */
public interface Mvp1ConsumptionRecordProvider extends AutoCloseable {
    /**
     * Data stream of all consumption records received by this region connector.
     *
     * @return consumption record stream
     */
    Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream();

    /**
     * Emit a complete signal on the Flow in this method.
     */
    @Override
    void close() throws Exception;
}
