package energy.eddie.api.agnostic;


import energy.eddie.api.v0.RegionConnector;

import java.util.concurrent.Flow;

/**
 * Used to extend a {@link RegionConnector} by making a Flow.Publisher of
 * {@link RawDataMessage}s available.
 */
public interface RawDataProvider extends AutoCloseable {
    /**
     * Stream of {@link RawDataMessage}s produced by this region connector.
     *
     * @return RawDataMessage stream.
     */
    Flow.Publisher<RawDataMessage> getRawDataStream();

    /**
     * Emit a complete signal on the Flow returned by {@link #getRawDataStream()} in this method.
     */
    @Override
    void close();
}
