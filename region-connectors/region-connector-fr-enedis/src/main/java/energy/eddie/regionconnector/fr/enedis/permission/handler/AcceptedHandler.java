package energy.eddie.regionconnector.fr.enedis.permission.handler;

import energy.eddie.regionconnector.fr.enedis.permission.events.FrAcceptedEvent;
import energy.eddie.regionconnector.fr.enedis.services.AccountingPointDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class AcceptedHandler implements EventHandler<FrAcceptedEvent> {
    private final AccountingPointDataService accountingPointDataService;

    public AcceptedHandler(
            AccountingPointDataService accountingPointDataService,
            EventBus eventBus
    ) {
        this.accountingPointDataService = accountingPointDataService;
        eventBus.filteredFlux(FrAcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(FrAcceptedEvent permissionEvent) {
        accountingPointDataService.fetchMeteringPointSegment(
                permissionEvent.permissionId(),
                permissionEvent.usagePointId()
        );
    }
}
