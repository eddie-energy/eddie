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
     */
    Flow.Publisher<ConnectionStatusMessage> connnectionStatusMessageStream();

    /**
     * Subscribe to the consumption record publisher to receive consumption records
     */
    Flow.Publisher<ConsumptionRecord> consumptionRecordStream();

}
