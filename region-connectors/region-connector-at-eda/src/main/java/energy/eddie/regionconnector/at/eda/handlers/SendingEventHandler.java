package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.permission.request.events.ExceptionEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class SendingEventHandler implements EventHandler<ValidatedEvent> {
    private final EdaAdapter edaAdapter;
    private final Outbox outbox;

    protected SendingEventHandler(
            EventBus eventBus,
            EdaAdapter edaAdapter,
            Outbox outbox
    ) {
        this.edaAdapter = edaAdapter;
        this.outbox = outbox;
        eventBus.filteredFlux(ValidatedEvent.class)
                .subscribe(this::threadedAccept);
    }

    private void threadedAccept(ValidatedEvent permissionEvent) {
        Thread.startVirtualThread(() -> accept(permissionEvent));
    }

    @Override
    public void accept(ValidatedEvent permissionEvent) {
        String permissionId = permissionEvent.permissionId();
        try {
            edaAdapter.sendCMRequest(permissionEvent.ccmoRequest());
        } catch (TransmissionException e) {
            outbox.commit(new ExceptionEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND, e));
        }
    }
}
