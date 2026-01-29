// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.event.sourcing;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the outbox pattern.
 * First persists the permission event to the event store and then emits it to the {@link EventBus}.
 *
 * @see <a href="https://microservices.io/patterns/data/transactional-outbox.html">Outbox Pattern</a>
 */
public class Outbox {
    private static final Logger LOGGER = LoggerFactory.getLogger(Outbox.class);

    private final EventBus eventBus;
    private final PermissionEventRepository repository;

    public Outbox(EventBus eventBus, PermissionEventRepository repository) {
        this.eventBus = eventBus;
        this.repository = repository;
    }

    /**
     * Persist and send a permission event to the event store and event bus.
     *
     * @param permissionEvent the event to be persisted and sent
     */
    public void commit(PermissionEvent permissionEvent) {
        if (repository.saveAndFlush(permissionEvent) != null) {
            eventBus.emit(permissionEvent);
        } else if (LOGGER.isErrorEnabled()) {
            LOGGER.error("Could not save event, it was not emitted and might not be repeatable: {}",
                        permissionEvent.permissionId());
        }
    }
}
