package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.StartPollingEvent;
import energy.eddie.regionconnector.de.eta.service.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler for permission acceptance events in the DE-ETA region connector.
 * <p>
 * This handler listens for:
 * <ul>
 *     <li>{@link AcceptedEvent} - emitted when a permission request is accepted</li>
 *     <li>{@link StartPollingEvent} - emitted by CommonFutureDataService to trigger periodic polling</li>
 * </ul>
 * <p>
 * When triggered, it initiates data polling for the associated permission request.
 */
@Component
public class AcceptedHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedHandler.class);

    private final PollingService pollingService;
    private final DePermissionRequestRepository repository;
    private final DataNeedsService dataNeedsService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AcceptedHandler(
            PollingService pollingService,
            DePermissionRequestRepository repository,
            EventBus eventBus,
            DataNeedsService dataNeedsService
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;

        // Subscribe to AcceptedEvent for initial data polling after permission acceptance
        eventBus.filteredFlux(AcceptedEvent.class)
                .subscribe(this::accept);

        // Subscribe to StartPollingEvent for periodic future data polling
        eventBus.filteredFlux(StartPollingEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        LOGGER.info("Accepted permission request {}, start polling data", permissionId);

        var optionalRequest = repository.findByPermissionId(permissionId);
        if (optionalRequest.isEmpty()) {
            LOGGER.warn("Permission request {} not found", permissionId);
            return;
        }

        DePermissionRequest permissionRequest = optionalRequest.get();
        var dataNeedId = permissionRequest.dataNeedId();
        var dataNeed = dataNeedsService.getById(dataNeedId);

        if (dataNeed instanceof ValidatedHistoricalDataDataNeed) {
            pollingService.pollTimeSeriesData(permissionRequest);
        } else {
            LOGGER.warn("Got unsupported data need {} for permission request {}", dataNeedId, permissionId);
        }
    }
}

