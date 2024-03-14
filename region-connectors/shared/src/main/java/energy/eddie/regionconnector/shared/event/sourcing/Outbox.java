package energy.eddie.regionconnector.shared.event.sourcing;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Outbox {
    private static final Logger LOGGER = LoggerFactory.getLogger(Outbox.class);

    private final EventBus eventBus;
    private final PermissionEventRepository repository;

    public Outbox(EventBus eventBus, PermissionEventRepository repository) {
        this.eventBus = eventBus;
        this.repository = repository;
    }

    public void commit(PermissionEvent permissionEvent) {
        if (repository.saveAndFlush(permissionEvent) != null) {
            eventBus.emit(permissionEvent);
        } else if (LOGGER.isErrorEnabled()) {
            LOGGER.error("Could not save event, it was not emitted and might not be repeatable: {}",
                        permissionEvent.permissionId());
        }
    }
}
