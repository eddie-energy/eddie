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
import java.util.Optional;

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
        Optional<DePermissionRequest> optionalPr = repository.findByPermissionId(event.permissionId());

        if (optionalPr.isEmpty()) {
            LOGGER.warn("Permission request not found for id: {}", event.permissionId());
            return;
        }

        DePermissionRequest pr = optionalPr.get();
        ZonedDateTime latestReading = event.latestReading();

        // 🟢 FIX 1: Update the Entity
        // We must update the state of the object so the test can see it.
        pr.setLatestReading(latestReading); // Assuming a setter exists

        // 🟢 FIX 2: Save to Database
        // Without this, the change is lost, and the test assertion fails.
        repository.save(pr);

        LocalDate end = pr.end();

        // Check for fulfillment logic
        if (latestReading != null) {
            // Logic to check if we are caught up
            boolean isFulfilled = !latestReading.toLocalDate().isBefore(end);

            if (isFulfilled) {
                LOGGER.atInfo()
                        .addArgument(pr::permissionId)
                        .addArgument(() -> latestReading)
                        .addArgument(() -> end)
                        .log("Permission request {} is fulfilled: latest reading {} >= end date {}");

                outbox.commit(new SimpleEvent(pr.permissionId(), PermissionProcessStatus.FULFILLED));
            }
        }
    }
}