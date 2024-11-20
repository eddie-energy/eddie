package energy.eddie.api.agnostic.outbound;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;

/**
 * A retransmission connector allows the eligible party to re-request data for a specific timeframe from a region-connector.
 */
public interface RetransmissionConnector {

    /**
     * Re-request data from a specific timeframe for a permission request of a region-connector.
     *
     * @param regionConnectorId     the region-connector that owns the permission request
     * @param retransmissionRequest Contains the start and end date of the timeframe that's re-requested and the permission request ID.
     */
    void retransmit(String regionConnectorId, RetransmissionRequest retransmissionRequest);
}
