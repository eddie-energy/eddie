package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.agnostic.data.needs.AccountingPointDataNeedResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.us.green.button.permission.events.UsStartPollingEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.services.PollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StartPollingEventHandler implements EventHandler<UsStartPollingEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartPollingEventHandler.class);
    private final PollingService pollingService;
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final UsPermissionRequestRepository repository;

    public StartPollingEventHandler(
            PollingService pollingService, EventBus eventBus, DataNeedCalculationService<DataNeed> calculationService,
            UsPermissionRequestRepository repository
    ) {
        this.pollingService = pollingService;
        this.calculationService = calculationService;
        this.repository = repository;
        eventBus.filteredFlux(UsStartPollingEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(UsStartPollingEvent event) {
        var permissionId = event.permissionId();
        var pr = repository.getByPermissionId(permissionId);
        var calc = calculationService.calculate(pr.dataNeedId(), pr.created());
        LOGGER.info("Starting to poll data for permission request {}", permissionId);
        switch(calc) {
            case ValidatedHistoricalDataDataNeedResult ignored ->  pollingService.pollValidatedHistoricalData(permissionId);
            case AccountingPointDataNeedResult ignored -> pollingService.pollAccountingPointData(pr);
            default -> LOGGER.warn("Invalid calculation {} for permission request {}", calc, permissionId);
        }
    }
}
