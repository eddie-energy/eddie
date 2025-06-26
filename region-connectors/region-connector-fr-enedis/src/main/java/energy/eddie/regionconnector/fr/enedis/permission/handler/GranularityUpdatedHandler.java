package energy.eddie.regionconnector.fr.enedis.permission.handler;

import energy.eddie.regionconnector.fr.enedis.permission.events.FrGranularityUpdateEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrUsagePointTypeEvent;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class GranularityUpdatedHandler implements EventHandler<FrGranularityUpdateEvent> {
    private final FrPermissionRequestRepository repository;
    private final Outbox outbox;

    public GranularityUpdatedHandler(EventBus eventBus, FrPermissionRequestRepository repository, Outbox outbox) {
        this.outbox = outbox;
        this.repository = repository;
        eventBus.filteredFlux(FrGranularityUpdateEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(FrGranularityUpdateEvent event) {
        var request = repository.getByPermissionId(event.permissionId());
        outbox.commit(new FrUsagePointTypeEvent(request.permissionId(), request.usagePointType()));
    }
}
