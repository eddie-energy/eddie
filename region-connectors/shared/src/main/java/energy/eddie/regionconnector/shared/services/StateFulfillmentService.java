package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateFulfillmentService implements FulfillmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateFulfillmentService.class);

    @Override
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
