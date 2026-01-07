package energy.eddie.regionconnector.es.datadis.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.AllowedGranularity;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEventForVHD;
import energy.eddie.regionconnector.es.datadis.permission.events.EsGranularityEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsUnfulfillableEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.services.HistoricalDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AcceptedVHDHandler implements EventHandler<EsAcceptedEventForVHD> {
    private final Outbox outbox;
    private final HistoricalDataService historicalDataService;
    private final EsPermissionRequestRepository repository;

    public AcceptedVHDHandler(
            EventBus eventBus,
            Outbox outbox,
            HistoricalDataService historicalDataService,
            EsPermissionRequestRepository repository
    ) {
        this.outbox = outbox;
        this.historicalDataService = historicalDataService;
        this.repository = repository;
        eventBus.filteredFlux(EsAcceptedEventForVHD.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(EsAcceptedEventForVHD permissionEvent) {
        var permissionRequest = repository.getByPermissionId(permissionEvent.permissionId());

        var permissionId = permissionEvent.permissionId();
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
        if (supportsQuarterHourly(allowedGranularity, pointType)) {
            return Optional.of(Granularity.PT15M);
        }

        // If point type is not 1 or 2, but hourly measurements are allowed
        if (allowedGranularity == AllowedGranularity.PT15M_OR_PT1H) {
            return Optional.of(Granularity.PT1H);
        }

        // If quarter hourly measurements are requested, but the point type does not support it
        return Optional.empty();
    }

    private static boolean supportsQuarterHourly(AllowedGranularity allowedGranularity, Integer pointType) {
        return (pointType == 1 || pointType == 2) &&
               (allowedGranularity == AllowedGranularity.PT15M
                || allowedGranularity == AllowedGranularity.PT15M_OR_PT1H);
    }
}
