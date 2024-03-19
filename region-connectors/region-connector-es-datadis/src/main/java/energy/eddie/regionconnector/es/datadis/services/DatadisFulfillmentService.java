package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * This service updates the last pulled meter reading and checks for fulfillment of the permission request.
 */
@Service
public class DatadisFulfillmentService extends FulfillmentService<EsPermissionRequest> {
    public DatadisFulfillmentService() {
        super(DatadisRegionConnectorMetadata.getInstance());
    }

    /**
     * Checks if the permission request is fulfilled by the given date.
     *
     * @param permissionRequest the permission request
     * @param date              the date to check against
     * @return true if {@code date} is >= {@link EsPermissionRequest#end()}. The {@link EsPermissionRequest#end()} is already inclusive, so we check >= instead of >.
     */
    @Override
    public boolean isPermissionRequestFulfilledByDate(EsPermissionRequest permissionRequest, ZonedDateTime date) {
        return Optional.ofNullable(permissionRequest.end())
                .map(ZonedDateTime::toLocalDate)
                .map(end -> !date.toLocalDate().isBefore(end))
                .orElse(false);
    }
}
