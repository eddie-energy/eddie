package energy.eddie.api.agnostic.outbound;

import energy.eddie.api.agnostic.RawDataMessage;
import reactor.core.publisher.Flux;

/**
 * Gets a raw data stream and emits the raw data messages to the eligible party.
 */
public interface RawDataOutboundConnector {
    /**
     * Sets a Flux that can be subscribed to, to emit raw data messages to the eligible party.
     *
     * @param rawDataStream flux containing raw data messages
     */
    void setRawDataStream(Flux<RawDataMessage> rawDataStream);
}
