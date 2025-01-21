package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.service.AcceptanceOrRejectionService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "region-connector.be.fluvius.mock-mandates", havingValue = "true")
public class MockMandateSentToPermissionAdminHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockMandateSentToPermissionAdminHandler.class);
    private final AcceptanceOrRejectionService acceptanceOrRejectionService;

    public MockMandateSentToPermissionAdminHandler(
            EventBus eventBus,
            AcceptanceOrRejectionService acceptanceOrRejectionService
    ) {
        eventBus.filteredFlux(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .subscribe(this::accept);
        LOGGER.info("'region-connector.be.fluvius.mock-mandates' set to true, auto accept all permission requests");
        this.acceptanceOrRejectionService = acceptanceOrRejectionService;
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        LOGGER.info("Accepting permission request {}", permissionId);
        try {
            acceptanceOrRejectionService.acceptOrRejectPermissionRequest(permissionId,
                                                                         PermissionProcessStatus.ACCEPTED);
        } catch (PermissionNotFoundException e) {
            LOGGER.warn("Got unknown permission {}", permissionId, e);
        }
    }
}
