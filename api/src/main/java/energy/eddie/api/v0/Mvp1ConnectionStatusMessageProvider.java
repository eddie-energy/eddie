package energy.eddie.api.v0;

import java.util.concurrent.Flow;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of
 * {@link ConnectionStatusMessage}s available.
 */
public interface Mvp1ConnectionStatusMessageProvider {
    /**
     * Data stream of all connection status updates created by this region connector.
     *
     * @return connection status message stream that can be consumed only once
     */
    Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream();
}
