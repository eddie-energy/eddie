// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.us.green.button.permission.events.UsAuthorizationUpdateFinishedEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestService;
import energy.eddie.regionconnector.us.green.button.services.historical.collection.HistoricalCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.ZoneOffset;


@Component
public class AuthorizationUpdateFinishedHandler implements EventHandler<UsAuthorizationUpdateFinishedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationUpdateFinishedHandler.class);
    private final HistoricalCollectionService historicalCollectionService;
    private final PermissionRequestService permissionRequestService;
    private final UsPermissionRequestRepository repository;

    public AuthorizationUpdateFinishedHandler(
            EventBus eventBus,
            HistoricalCollectionService historicalCollectionService,
            PermissionRequestService permissionRequestService,
            UsPermissionRequestRepository repository
    ) {
        this.historicalCollectionService = historicalCollectionService;
        this.permissionRequestService = permissionRequestService;
        this.repository = repository;
        eventBus.filteredFlux(UsAuthorizationUpdateFinishedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(UsAuthorizationUpdateFinishedEvent event) {
        var permissionId = event.permissionId();
        var permissionRequest = repository.getByPermissionId(permissionId);
        var now = LocalDate.now(ZoneOffset.UTC);
        historicalCollectionService.persistMetersForPermissionRequest(permissionRequest)
                                   .filter(meterReading -> filterInactivePermissionRequest(permissionRequest, now))
                                   .filter(meterReading -> !meterReading.isReadyToPoll())
                                   .collectList()
                                   .filter(list -> !list.isEmpty())
                                   .flatMap(meters -> historicalCollectionService.triggerHistoricalDataCollection(
                                           meters,
                                           permissionRequest
                                   ))
                                   .publishOn(Schedulers.boundedElastic())
                                   .doFinally(
                                           v -> permissionRequestService.removeUnfulfillablePermissionRequest(
                                                   permissionId
                                           )
                                   )
                                   .subscribe();
    }

    private static boolean filterInactivePermissionRequest(
            UsGreenButtonPermissionRequest permissionRequest,
            LocalDate now
    ) {
        var permissionId = permissionRequest.permissionId();
        if (permissionRequest.start().isAfter(now)) {
            LOGGER.info("Permission request {} is not active yet, will not trigger historical collection",
                        permissionId);
            return false;
        }
        return true;
    }
}
