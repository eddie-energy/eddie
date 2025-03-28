package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.events.DataReceivedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataReceivedHandler implements EventHandler<DataReceivedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataReceivedHandler.class);
    private final FulfillmentService fulfillmentService;
    private final AtPermissionRequestRepository repository;

    public DataReceivedHandler(
            FulfillmentService fulfillmentService,
            EventBus eventBus,
            AtPermissionRequestRepository repository
    ) {
        this.fulfillmentService = fulfillmentService;
        this.repository = repository;
        eventBus.filteredFlux(DataReceivedEvent.class).subscribe(this::accept);
    }

    @Override
    public void accept(DataReceivedEvent event) {
        if (isTerminalState(event.status())) {
            return;
        }
        var pr = repository.getByPermissionId(event.permissionId());

        LOGGER.atDebug()
              .addArgument(event::permissionId)
              .addArgument(pr::end)
              .addArgument(event::end)
              .log("Checking if permission request {} is fulfilled. Permission end date: {}, metering period end date: {}");

        // if we request quarter hourly data up to the 24.01.2024, the last consumption record we get will have a meteringPeriodStart of 24.01.2024T23:45:00 and a meteringPeriodEnd of 25.01.2024T00:00:00
        // so if the permissionEnd is before the meteringPeriodEnd the permission request is fulfilled
        if (fulfillmentService.isPermissionRequestFulfilledByDate(pr, event.end())) {
            fulfillmentService.tryFulfillPermissionRequest(pr);
        }
    }

    private boolean isTerminalState(PermissionProcessStatus status) {
        return status == PermissionProcessStatus.TERMINATED
               || status == PermissionProcessStatus.REVOKED
               || status == PermissionProcessStatus.FULFILLED
               || status == PermissionProcessStatus.INVALID
               || status == PermissionProcessStatus.MALFORMED
               || status == PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION
               || status == PermissionProcessStatus.FAILED_TO_TERMINATE
               || status == PermissionProcessStatus.EXTERNALLY_TERMINATED;
    }
}
