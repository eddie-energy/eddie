package energy.eddie.regionconnector.dk.energinet.permission.handler;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkAcceptedEvent;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.services.AccountingPointDetailsService;
import energy.eddie.regionconnector.dk.energinet.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class AcceptanceHandler implements EventHandler<DkAcceptedEvent> {

    private final PollingService pollingService;
    private final AccountingPointDetailsService accountingPointDetailsService;
    private final DataNeedsService dataNeedsService;
    private final DkPermissionRequestRepository repository;

    public AcceptanceHandler(
            EventBus eventBus,
            PollingService pollingService,
            AccountingPointDetailsService accountingPointDetailsService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService,
            DkPermissionRequestRepository repository
    ) {
        this.pollingService = pollingService;
        this.accountingPointDetailsService = accountingPointDetailsService;
        this.dataNeedsService = dataNeedsService;
        this.repository = repository;
        eventBus.filteredFlux(DkAcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void accept(DkAcceptedEvent permissionEvent) {
        var pr = repository.getByPermissionId(permissionEvent.permissionId());

        var dataNeed = dataNeedsService.findById(pr.dataNeedId()).orElseThrow(EntityNotFoundException::new);

        switch (dataNeed) {
            case AccountingPointDataNeed ignored -> accountingPointDetailsService.fetchMeteringPointDetails(pr);
            default -> pollingService.fetchHistoricalMeterReadings(pr);
        }
    }
}
