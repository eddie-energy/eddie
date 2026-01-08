package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Event handler that checks for fulfillment when the latest meter reading is updated.
 * If the latest meter reading date is after or equal to the permission request end date,
 * the permission request is fulfilled.
 */
@Component
public class LatestMeterReadingEventHandler implements EventHandler<LatestMeterReadingEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LatestMeterReadingEventHandler.class);

    private final Outbox outbox;
    private final DePermissionRequestRepository repository;

    public LatestMeterReadingEventHandler(
            EventBus eventBus,
            Outbox outbox,
            DePermissionRequestRepository repository
    ) {
        this.outbox = outbox;
        this.repository = repository;
        eventBus.filteredFlux(LatestMeterReadingEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(LatestMeterReadingEvent event) {
        var optionalPr = repository.findByPermissionId(event.permissionId());
        if (optionalPr.isEmpty()) {
            LOGGER.warn("Permission request not found for id: {}", event.permissionId());
            return;
        }

        DePermissionRequest pr = optionalPr.get();
        ZonedDateTime latestReading = event.latestReading();
        LocalDate end = pr.end();

        if (latestReading != null) {
            LOGGER.atInfo()
                  .addArgument(pr::permissionId)
                  .addArgument(() -> latestReading)
                  .addArgument(() -> end)
                  .log("Permission request {} is fulfilled: latest reading {} >= end date {}");

            outbox.commit(new SimpleEvent(pr.permissionId(), PermissionProcessStatus.FULFILLED));
        } else {
            LOGGER.atDebug()
                  .addArgument(pr::permissionId)
                  .addArgument(() -> latestReading)
                  .addArgument(() -> end)
                  .log("Permission request {} not yet fulfilled: latest reading {} < end date {}");
        }
    }
}

