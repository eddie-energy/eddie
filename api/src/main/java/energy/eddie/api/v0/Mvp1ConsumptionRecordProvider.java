package energy.eddie.api.v0;

import java.util.concurrent.Flow;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of
 * {@link ConsumptionRecord}s available.
 */
public interface Mvp1ConsumptionRecordProvider {
    /**
     * Data stream of all consumption records received by this region connector.
     *
     * @return consumption record stream that can be consumed only once
     */
    Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream();
}
