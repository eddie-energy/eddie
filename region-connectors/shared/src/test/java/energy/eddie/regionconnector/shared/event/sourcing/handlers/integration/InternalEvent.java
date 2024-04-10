package energy.eddie.regionconnector.shared.event.sourcing.handlers.integration;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

record InternalEvent(String permissionId, PermissionProcessStatus status,
                     ZonedDateTime eventCreated) implements PermissionEvent, InternalPermissionEvent {
    InternalEvent(String permissionId, PermissionProcessStatus status) {
        this(permissionId, status, ZonedDateTime.now(ZoneOffset.UTC));
    }
}
