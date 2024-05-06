package energy.eddie.regionconnector.es.datadis.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.services.HistoricalDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AcceptedHandler implements EventHandler<EsAcceptedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedHandler.class);
    private final HistoricalDataService historicalDataService;
    private final EsPermissionRequestRepository repository;

    public AcceptedHandler(
            EventBus eventBus,
            HistoricalDataService historicalDataService,
            EsPermissionRequestRepository repository
    ) {
        this.historicalDataService = historicalDataService;
        this.repository = repository;
        eventBus.filteredFlux(EsAcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(EsAcceptedEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var pr = repository.findByPermissionId(permissionId);
        if (pr.isEmpty()) {
            LOGGER.error("Got unknown permission request {}", permissionId);
            return;
        }
        var permissionRequest = pr.get();
        // Have to rebuild permission request, since Hibernate might cache old query results
        historicalDataService.fetchAvailableHistoricalData(new DatadisPermissionRequest(
                permissionId,
                permissionRequest.connectionId(),
                permissionRequest.dataNeedId(),
                permissionRequest.granularity(),
                permissionRequest.nif(),
                permissionRequest.meteringPointId(),
                permissionRequest.start(),
                permissionRequest.end(),
                permissionEvent.distributorCode(),
                permissionEvent.supplyPointType(),
                permissionRequest.latestMeterReadingEndDate().orElse(null),
                PermissionProcessStatus.ACCEPTED,
                permissionRequest.errorMessage(),
                permissionEvent.isProductionSupport(),
                permissionRequest.created()
        ));
    }
}
