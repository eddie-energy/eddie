package energy.eddie.api.agnostic;


import energy.eddie.api.v0.RegionConnector;
import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of
 * {@link RawDataMessage}s available.
 */
public interface RawDataProvider extends AutoCloseable {
    /**
     * Stream of {@link RawDataMessage}s produced by this region connector.
     *
     * @return RawDataMessage stream.
     */
    Flux<RawDataMessage> getRawDataStream();

    /**
     * Emit a complete signal on the Flow returned by {@link #getRawDataStream()} in this method.
     */
    @Override
    void close();
}
