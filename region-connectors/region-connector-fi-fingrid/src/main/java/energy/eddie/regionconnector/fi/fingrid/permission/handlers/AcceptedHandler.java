package energy.eddie.regionconnector.fi.fingrid.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.fi.fingrid.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AcceptedHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedHandler.class);
    private final PollingService pollingService;
    private final FiPermissionRequestRepository repository;
    private final DataNeedsService dataNeedsService;

    public AcceptedHandler(
            PollingService pollingService,
            FiPermissionRequestRepository repository,
            EventBus eventBus,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
        eventBus.filteredFlux(AcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        LOGGER.info("Accepted permission request {}, start polling data", permissionEvent);
        var permissionId = permissionEvent.permissionId();
        var permissionRequest = repository.getByPermissionId(permissionId);
        var dataNeedId = permissionRequest.dataNeedId();
        var calc = dataNeedsService.getById(dataNeedId);
        switch (calc) {
            case ValidatedHistoricalDataDataNeed ignored -> pollingService.pollTimeSeriesData(permissionRequest);
            case AccountingPointDataNeed ignored -> pollingService.pollAccountingPointData(permissionRequest);
            default -> LOGGER.warn("Got invalid data need {} for permission request {}", dataNeedId, permissionId);
        }
    }
}
