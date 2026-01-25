package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeValidatedEventHandler implements EventHandler<ValidatedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeValidatedEventHandler.class);
    private final DePermissionRequestRepository repository;

    public DeValidatedEventHandler(EventBus eventBus, DePermissionRequestRepository repository) {
        this.repository = repository;
        eventBus.filteredFlux(ValidatedEvent.class).subscribe(this::accept);
    }

    @Override
    public void accept(ValidatedEvent event) {
        repository.findByPermissionId(event.permissionId()).ifPresentOrElse(pr -> {
            var updated = DePermissionRequest.builder()
                    .permissionId(pr.permissionId())
                    .connectionId(pr.connectionId())
                    .meteringPointId(pr.meteringPointId())
                    // UPDATE: Set the dates from the event
                    .start(event.start())
                    .end(event.end())
                    .granularity(event.granularity())
                    .energyType(pr.energyType())
                    .status(pr.status())
                    .created(pr.created())
                    .dataNeedId(pr.dataNeedId())
                    .latestReading(pr.latestReading().orElse(null))
                    .build();

            repository.save(updated);
            LOGGER.info("Updated permission {} with validated dates: {} to {}",
                    pr.permissionId(), event.start(), event.end());
        }, () -> LOGGER.warn("Permission {} not found for validation", event.permissionId()));
    }
}