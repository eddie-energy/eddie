package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "region-connector.be.fluvius.mock-mandates", havingValue = "true")
public class MockMandateSentToPermissionAdminHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockMandateSentToPermissionAdminHandler.class);
    private final Outbox outbox;

    public MockMandateSentToPermissionAdminHandler(Outbox outbox, EventBus eventBus) {
        this.outbox = outbox;
        eventBus.filteredFlux(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .subscribe(this::accept);
        LOGGER.info("'region-connector.be.fluvius.mock-mandates' set to true, auto accept all permission requests");
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        LOGGER.info("Accepting permission request {}", permissionId);
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.ACCEPTED));
    }
}
