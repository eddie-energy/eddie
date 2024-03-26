package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class FulfillmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FulfillmentService.class);

    /**
     * Checks if the permission request is fulfilled by the given date.
     *
     * @param permissionRequest the permission request
     * @param date              the date to check against
     * @return true if {@code date} is after {@link PermissionRequest#end()}
     */
    public boolean isPermissionRequestFulfilledByDate(PermissionRequest permissionRequest, LocalDate date) {
        return date.isAfter(permissionRequest.end());
    }

    /**
     * Tries to fulfill the permission request.
     *
     * @param permissionRequest the permission request
     */
    public void tryFulfillPermissionRequest(PermissionRequest permissionRequest) {
        LOGGER.atInfo()
              .addArgument(() -> permissionRequest.dataSourceInformation().regionConnectorId())
              .addArgument(permissionRequest::permissionId)
              .log("{}: Fulfilling permission request {}");
        try {
            permissionRequest.fulfill();
            LOGGER.atInfo()
                  .addArgument(() -> permissionRequest.dataSourceInformation().regionConnectorId())
                  .addArgument(permissionRequest::permissionId)
                  .log("{}: Permission request {} fulfilled");
        } catch (StateTransitionException e) {
            LOGGER.atError()
                  .addArgument(() -> permissionRequest.dataSourceInformation().regionConnectorId())
                  .addArgument(permissionRequest::permissionId)
                  .log("{}: Error while fulfilling permission request {}", e);
        }
    }
}
