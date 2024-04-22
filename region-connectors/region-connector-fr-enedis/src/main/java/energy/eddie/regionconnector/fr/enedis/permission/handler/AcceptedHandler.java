package energy.eddie.regionconnector.fr.enedis.permission.handler;

import energy.eddie.regionconnector.fr.enedis.permission.events.FrAcceptedEvent;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.services.HistoricalDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AcceptedHandler implements EventHandler<FrAcceptedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedHandler.class);
    private final HistoricalDataService historicalDataService;
    private final FrPermissionRequestRepository repository;

    public AcceptedHandler(
            HistoricalDataService historicalDataService,
            EventBus eventBus,
            FrPermissionRequestRepository repository
    ) {
        this.historicalDataService = historicalDataService;
        this.repository = repository;
        eventBus.filteredFlux(FrAcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(FrAcceptedEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var pr = repository.findByPermissionId(permissionId);
        if (pr.isEmpty()) {
            LOGGER.error("Got unknown permission request {}", permissionId);
            return;
        }
        historicalDataService.fetchHistoricalMeterReadings(pr.get(), permissionEvent.usagePointId());
    }
}
