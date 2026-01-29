// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlInternalPollingEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Component
public class InternalPollingHandler implements EventHandler<NlInternalPollingEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalPollingHandler.class);
    private final NlPermissionRequestRepository repository;
    private final Outbox outbox;

    public InternalPollingHandler(EventBus eventBus, NlPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;

        eventBus.filteredFlux(NlInternalPollingEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(NlInternalPollingEvent permissionEvent) {
        String permissionId = permissionEvent.permissionId();
        var pr = repository.findByPermissionId(permissionId);
        if (pr.isEmpty()) {
            LOGGER.error("Got unknown permission request {}", permissionId);
            return;
        }
        var permissionRequest = pr.get();
        var isFulfilled = permissionEvent.lastMeterReadings().values().stream()
                                         .allMatch(timestamp -> isAfter(timestamp, permissionRequest.end()));
        if (isFulfilled) {
            outbox.commit(new NlSimpleEvent(permissionId, PermissionProcessStatus.FULFILLED));
        }
    }

    private static boolean isAfter(ZonedDateTime timestamp, LocalDate end) {
        var eod = DateTimeUtils.endOfDay(end, timestamp.getZone());
        return timestamp.isAfter(eod);
    }
}
