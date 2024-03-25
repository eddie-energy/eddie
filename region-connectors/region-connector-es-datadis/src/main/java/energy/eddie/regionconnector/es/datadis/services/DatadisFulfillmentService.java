package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import org.springframework.stereotype.Service;

/**
 * This service updates the last pulled meter reading and checks for fulfillment of the permission request.
 */
@Service
public class DatadisFulfillmentService extends FulfillmentService<EsPermissionRequest> {
    public DatadisFulfillmentService() {
        super(DatadisRegionConnectorMetadata.getInstance());
    }
}
