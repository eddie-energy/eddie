package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TerminationHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationHandler.class);
    private final Outbox outbox;
    private final AtPermissionRequestRepository repository;
    private final AtConfiguration atConfiguration;
    private final EdaAdapter edaAdapter;

    public TerminationHandler(
            Outbox outbox, EventBus eventBus, AtPermissionRequestRepository repository,
            AtConfiguration atConfiguration, EdaAdapter edaAdapter
    ) {
        this.outbox = outbox;
        this.repository = repository;
        this.atConfiguration = atConfiguration;
        this.edaAdapter = edaAdapter;
        eventBus.filteredFlux(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var request = repository.findByPermissionId(permissionId);
        if (request.isEmpty()) {
            LOGGER.warn("No permission with this id found: {}", permissionId);
            return;
        }
        AtPermissionRequest permissionRequest = request.get();
        var revoke = new CCMORevoke(permissionRequest,
                                    atConfiguration.eligiblePartyId(),
                                    "Terminated by the Eligible Party");
        try {
            edaAdapter.sendCMRevoke(revoke);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.EXTERNALLY_TERMINATED));
        } catch (Exception e) {
            LOGGER.warn("Error trying to terminate permission request.", e);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FAILED_TO_TERMINATE));
        }
    }
}
