package energy.eddie.regionconnector.api.v0;

import energy.eddie.regionconnector.api.v0.models.ConsumptionRecord;

import java.util.concurrent.Flow;

public interface RegionConnector {
    /**
     * Revoke a permission
     * @param permissionId unique id of the permission
     */
    void revokePermission(String permissionId);

    /**
     * Subscribe to the connection status message publisher to receive updates on the status of permissions
     * @param subscriber the subscriber to subscribe to the connection status message publisher
     */
    void subscribeToConnectionStatusMessagePublisher(Flow.Subscriber<ConnectionStatusMessage> subscriber);

    /**
     * Subscribe to the consumption record publisher to receive consumption records
     * @param subscriber the subscriber to subscribe to the consumption record publisher
     */
    void subscribeToConsumptionRecordPublisher(Flow.Subscriber<ConsumptionRecord> subscriber);

}
