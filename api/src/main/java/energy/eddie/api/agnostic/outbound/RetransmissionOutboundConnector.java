package energy.eddie.api.agnostic.outbound;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import reactor.core.publisher.Flux;

/**
 * A retransmission connector allows the eligible party to request retransmission of data for a specific timeframe from a region-connector.
 */
public interface RetransmissionOutboundConnector {
    /**
     * A flux of {@link RetransmissionRequest}s. This is consumed by {@link energy.eddie.api.agnostic.retransmission.RetransmissionRequestRouter} to route the retransmission requests to the correct region-connector.
     *
     * @return Contains the regionConnectorId, the permissionRequestId and the start and end date of the timeframe that should be retransmitted.
     */
    Flux<RetransmissionRequest> retransmissionRequests();

    /**
     * Sets the stream of {@link RetransmissionRequest} to be sent to the EP app. This stream will be provided by the {@link energy.eddie.api.agnostic.retransmission.RetransmissionRequestRouter} .
     *
     * @param retransmissionResultStream stream of retransmission results
     */
    void setRetransmissionResultStream(Flux<RetransmissionResult> retransmissionResultStream);
}
