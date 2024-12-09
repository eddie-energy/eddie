package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.permission.events.UsAuthorizationUpdateFinishedEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestService;
import energy.eddie.regionconnector.us.green.button.services.historical.collection.HistoricalCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.StreamSupport;


@Component
public class AuthorizationUpdateFinishedHandler implements EventHandler<List<UsAuthorizationUpdateFinishedEvent>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationUpdateFinishedHandler.class);
    private final HistoricalCollectionService historicalCollectionService;
    private final PermissionRequestService permissionRequestService;
    private final UsPermissionRequestRepository repository;

    public AuthorizationUpdateFinishedHandler(
            EventBus eventBus,
            GreenButtonConfiguration config,
            HistoricalCollectionService historicalCollectionService,
            PermissionRequestService permissionRequestService,
            UsPermissionRequestRepository repository
    ) {
        this.historicalCollectionService = historicalCollectionService;
        this.permissionRequestService = permissionRequestService;
        this.repository = repository;
        eventBus.filteredFlux(UsAuthorizationUpdateFinishedEvent.class)
                .buffer(config.activationBatchSize())
                .subscribe(this::accept);
    }

    @Override
    public void accept(List<UsAuthorizationUpdateFinishedEvent> events) {
        var permissionIds = events.stream().map(PermissionEvent::permissionId).toList();
        var prs = repository.findAllById(permissionIds);
        // copyOf doesn't copy the list since toList() returns an unmodifiable list
        List<UsGreenButtonPermissionRequest> permissionRequests = List.copyOf(
                StreamSupport.stream(prs.spliterator(), false)
                             .toList()
        );
        var now = LocalDate.now(ZoneOffset.UTC);
        historicalCollectionService.persistMetersForPermissionRequests(permissionRequests)
                                   .filter(meterReading -> filterInactivePermissionRequests(
                                           meterReading, permissionRequests, now
                                   ))
                                   .filter(meterReading -> !meterReading.isReadyToPoll())
                                   .collectList()
                                   .filter(list -> !list.isEmpty())
                                   .flatMap(historicalCollectionService::triggerHistoricalDataCollection)
                                   .publishOn(Schedulers.boundedElastic())
                                   .doFinally(
                                           v -> permissionRequestService.removeUnfulfillablePermissionRequests(
                                                   permissionIds
                                           )
                                   )
                                   .subscribe();
    }

    private static boolean filterInactivePermissionRequests(
            MeterReading meterReading,
            List<UsGreenButtonPermissionRequest> permissionRequests,
            LocalDate now
    ) {
        for (var request : permissionRequests) {
            var permissionId = request.permissionId();
            if (!permissionId.equals(meterReading.permissionId())) {
                continue;
            }
            if (request.start().isAfter(now)) {
                LOGGER.info("Permission request {} is not active yet, will not trigger historical collection",
                            permissionId);
                return false;
            } else {
                return true;
            }
        }
        // Impossible to end up here, since a permission request is required to create a meter reading
        throw new IllegalStateException(
                "Got meter reading without matching permission request %s".formatted(meterReading.permissionId())
        );
    }
}
