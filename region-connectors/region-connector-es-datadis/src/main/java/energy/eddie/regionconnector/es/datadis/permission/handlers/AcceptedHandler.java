package energy.eddie.regionconnector.es.datadis.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.AllowedGranularity;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsGranularityEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsUnfulfillableEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.services.HistoricalDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AcceptedHandler implements EventHandler<EsAcceptedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedHandler.class);
    private final Outbox outbox;
    private final HistoricalDataService historicalDataService;
    private final EsPermissionRequestRepository repository;

    public AcceptedHandler(
            EventBus eventBus,
            Outbox outbox,
            HistoricalDataService historicalDataService,
            EsPermissionRequestRepository repository
    ) {
        this.outbox = outbox;
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

        var granularity = granularity(permissionRequest.allowedGranularity(), permissionEvent.supplyPointType());
        if (granularity.isEmpty()) {
            outbox.commit(new EsUnfulfillableEvent(permissionId, "Metering point can`t provide requested granularity"));
            return;
        }

        outbox.commit(new EsGranularityEvent(permissionId, granularity.get()));

        // Have to rebuild permission request, since Hibernate might cache old query results
        historicalDataService.fetchAvailableHistoricalData(new DatadisPermissionRequest(
                permissionId,
                permissionRequest.connectionId(),
                permissionRequest.dataNeedId(),
                granularity.get(),
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
                permissionRequest.created(),
                permissionRequest.allowedGranularity()
        ));
    }

    private Optional<Granularity> granularity(AllowedGranularity allowedGranularity, Integer pointType) {
        // All point types support hourly data
        if (allowedGranularity == AllowedGranularity.PT1H) {
            return Optional.of(Granularity.PT1H);
        }

        // Only point types 1 and 2 support quarter hourly data
        if ((pointType == 1 || pointType == 2) &&
                (allowedGranularity == AllowedGranularity.PT15M
                        || allowedGranularity == AllowedGranularity.PT15M_OR_PT1H)) {
            return Optional.of(Granularity.PT15M);
        }

        // If point type is not 1 or 2, but hourly measurements are allowed
        if (allowedGranularity == AllowedGranularity.PT15M_OR_PT1H) {
            return Optional.of(Granularity.PT1H);
        }

        // If quarter hourly measurements are requested, but the point type does not support it
        return Optional.empty();
    }
}
