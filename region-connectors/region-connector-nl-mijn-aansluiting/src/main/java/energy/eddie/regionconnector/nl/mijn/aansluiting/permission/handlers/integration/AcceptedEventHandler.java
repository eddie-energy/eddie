package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.handlers.integration;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
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
    private final DataNeedsService dataNeedsService;

    public AcceptedEventHandler(
            EventBus eventBus,
            PollingService pollingService,
            NlPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
        eventBus.filteredFlux(PermissionProcessStatus.ACCEPTED)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        if (permissionEvent instanceof InternalPermissionEvent) {
            return;
        }
        String permissionId = permissionEvent.permissionId();
        var request = repository.getByPermissionId(permissionId);
        var dataNeed = dataNeedsService.getById(request.dataNeedId());
        switch (dataNeed) {
            case AccountingPointDataNeed ignored -> pollingService.fetchAccountingPointData(request);
            case ValidatedHistoricalDataDataNeed ignored -> pollValidatedHistoricalData(request);
            default -> LOGGER.warn("Unsupported data need: {}", dataNeed);
        }
    }

    private void pollValidatedHistoricalData(MijnAansluitingPermissionRequest request) {
        if (request.start().isBefore(LocalDate.now(NL_ZONE_ID))) {
            LOGGER.atInfo()
                  .addArgument(request::permissionId)
                  .log("Fetching data for accepted permission request {}");
            pollingService.pollTimeSeriesData(request);
        }
    }
}
