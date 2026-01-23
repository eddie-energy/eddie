// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.events.MeterReadingUpdatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class MeterReadingUpdatedEventHandler implements EventHandler<MeterReadingUpdatedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterReadingUpdatedEventHandler.class);
    private final Outbox outbox;
    private final BePermissionRequestRepository permissionRequestRepository;

    public MeterReadingUpdatedEventHandler(
            Outbox outbox,
            BePermissionRequestRepository permissionRequestRepository,
            EventBus eventBus
    ) {
        this.outbox = outbox;
        this.permissionRequestRepository = permissionRequestRepository;
        eventBus.filteredFlux(MeterReadingUpdatedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(MeterReadingUpdatedEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var permissionRequest = permissionRequestRepository.getByPermissionId(permissionId);
        var meterReadings = permissionRequest.lastMeterReadings();
        var end = permissionRequest.end().atStartOfDay(ZoneOffset.UTC);
        var isFulfilled = meterReadings.stream()
                                       .allMatch(reading -> readingIsAfter(reading, end, permissionRequest));
        if (isFulfilled) {
            LOGGER.info("Marking permission request {} as fulfilled", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FULFILLED));
        }
    }

    private static boolean readingIsAfter(
            MeterReading reading,
            ZonedDateTime end,
            FluviusPermissionRequest permissionRequest
    ) {
        var readingTimestamp = reading.lastMeterReading();
        if (readingTimestamp == null) {
            return false;
        }
        if (permissionRequest.granularity() != Granularity.P1D) {
            return !readingTimestamp.isBefore(end);
        }
        return !readingTimestamp.toLocalDate().isBefore(end.minusDays(1).toLocalDate());
    }
}
