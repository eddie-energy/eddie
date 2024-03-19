package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Optional;

public class FulfillmentService<T extends TimeframedPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FulfillmentService.class);
    private final String regionConnectorId;

    public FulfillmentService(RegionConnectorMetadata regionConnectorMetadata) {
        regionConnectorId = regionConnectorMetadata.id();
    }

    /**
     * Checks if the permission request is fulfilled by the given date.
     *
     * @param permissionRequest the permission request
     * @param date              the date to check against
     * @return true if {@code date} is after {@link TimeframedPermissionRequest#end()}
     */
    public boolean isPermissionRequestFulfilledByDate(T permissionRequest, ZonedDateTime date) {
        return Optional.ofNullable(permissionRequest.end())
                .map(date::isAfter)
                .orElse(false);
    }

    /**
     * Tries to fulfill the permission request.
     *
     * @param permissionRequest the permission request
     */
    public void tryFulfillPermissionRequest(T permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("{}: Fulfilling permission request {}", regionConnectorId, permissionId);
        try {
            permissionRequest.fulfill();
            LOGGER.info("{}: Permission request {} fulfilled", regionConnectorId, permissionId);
        } catch (StateTransitionException e) {
            LOGGER.error("{}: Error while fulfilling permission request {}", regionConnectorId, permissionId, e);
        }
    }
}
