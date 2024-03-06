package energy.eddie.api.v0;

import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of
 * {@link ConnectionStatusMessage}s available.
 */
public interface Mvp1ConnectionStatusMessageProvider extends AutoCloseable {
    /**
     * Data stream of all connection status updates created by this region connector.
     *
     * @return connection status message stream
     */
    Flux<ConnectionStatusMessage> getConnectionStatusMessageStream();

    /**
     * Emit a complete signal on the Flow in this method.
     */
    @Override
    void close() throws Exception;
}
