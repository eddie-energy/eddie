package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.regionconnector.de.eta.permission.events.FulfilledEvent;
import energy.eddie.regionconnector.de.eta.permission.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.Optional;

@Component
public class LatestMeterReadingEventHandler implements EventHandler<LatestMeterReadingEvent> {
    private final Outbox outbox;
    private final DeEtaPermissionRequestRepository repo;

    public LatestMeterReadingEventHandler(EventBus eventBus, Outbox outbox, DeEtaPermissionRequestRepository repo) {
        this.outbox = outbox;
        this.repo = repo;
        eventBus.filteredFlux(LatestMeterReadingEvent.class)
                .subscribe(this::accept);
    }

    public void accept(LatestMeterReadingEvent event) {
        Optional.ofNullable(repo.getByPermissionId(event.permissionId()))
                .filter(pr -> !DateTimeUtils.endOfDay(pr.end(), ZoneOffset.UTC).isAfter(event.latestReading()))
                .ifPresent(pr -> outbox.commit(new FulfilledEvent(pr.permissionId())));
    }
}