package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Component
public class AcceptedHandler implements EventHandler<AcceptedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedHandler.class);

    private final DePermissionRequestRepository repository;

    public AcceptedHandler(
            EventBus eventBus,
            DePermissionRequestRepository repository
    ) {
        this.repository = repository;
        eventBus.filteredFlux(AcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(AcceptedEvent event) {
        Optional<DePermissionRequest> optionalPr = repository.findByPermissionId(event.permissionId());
        if (optionalPr.isEmpty()) {
            LOGGER.warn("Permission request not found for id: {}", event.permissionId());
            return;
        }

        DePermissionRequest pr = optionalPr.get();

        if (pr.start().isAfter(LocalDate.now(ZoneId.of("UTC")))) {
            LOGGER.info("Permission request {} is for future data only.", pr.permissionId());
        } else {
            LOGGER.info("Permission request {} accepted. Awaiting Scheduled Provider to fetch data.", pr.permissionId());
        }
    }
}