// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.event.sourcing;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record TestPollingEvent(String permissionId,
                               PermissionProcessStatus status,
                               LocalDate lastPolled,
                               ZonedDateTime eventCreated) implements PermissionEvent, InternalPermissionEvent {
    public TestPollingEvent(String permissionId, PermissionProcessStatus status, LocalDate lastPolled) {
        this(permissionId, status, lastPolled, ZonedDateTime.now(ZoneOffset.UTC));
    }
}
