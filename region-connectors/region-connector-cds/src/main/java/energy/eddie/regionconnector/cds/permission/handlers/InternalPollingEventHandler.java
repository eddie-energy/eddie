// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.InternalPollingEvent;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

@Component
public class InternalPollingEventHandler implements EventHandler<InternalPollingEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalPollingEventHandler.class);
    private final CdsPermissionRequestRepository repository;
    private final Outbox outbox;

    public InternalPollingEventHandler(EventBus eventBus, CdsPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
        eventBus.filteredFlux(InternalPollingEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(InternalPollingEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var pr = repository.getByPermissionId(permissionId);
        var end = endOfDay(pr.end(), ZoneOffset.UTC);
        for (var entry : permissionEvent.lastMeterReadings().entrySet()) {
            if (entry.getValue().isBefore(end)) {
                LOGGER.info("Permission request {} is not fulfilled yet", permissionId);
                return;
            }
        }
        LOGGER.info("Permission request {} is fulfilled", permissionId);
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FULFILLED));
    }
}
