package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.handlers.integration;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.NL_ZONE_ID;

@Component
public class AcceptedEventHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedEventHandler.class);
    private final PollingService pollingService;
    private final NlPermissionRequestRepository repository;

    public AcceptedEventHandler(
            EventBus eventBus,
            PollingService pollingService,
            NlPermissionRequestRepository repository
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        eventBus.filteredFlux(PermissionProcessStatus.ACCEPTED)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        if (permissionEvent instanceof InternalPermissionEvent) {
            return;
        }
        String permissionId = permissionEvent.permissionId();
        var request = repository.findByPermissionId(permissionId);
        if (request.isEmpty()) {
            LOGGER.error("Got unknown accepted permission request: {}", permissionId);
            return;
        }
        var nlPermissionRequest = request.get();
        if (nlPermissionRequest.start().isBefore(LocalDate.now(NL_ZONE_ID))) {
            LOGGER.info("Fetching data for accepted permission request {}", permissionId);
            pollingService.fetchConsumptionData(nlPermissionRequest);
        }
    }
}
