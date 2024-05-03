package energy.eddie.regionconnector.dk.energinet.permission.handler;

import energy.eddie.regionconnector.dk.energinet.permission.events.DkAcceptedEvent;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class AcceptanceHandler implements EventHandler<DkAcceptedEvent> {

    private final PollingService pollingService;
    private final DkPermissionRequestRepository repository;

    public AcceptanceHandler(
            EventBus eventBus,
            PollingService pollingService,
            DkPermissionRequestRepository repository
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        eventBus.filteredFlux(DkAcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void accept(DkAcceptedEvent permissionEvent) {
        var pr = repository.getByPermissionId(permissionEvent.permissionId());
        pollingService.fetchHistoricalMeterReadings(pr);
    }
}
