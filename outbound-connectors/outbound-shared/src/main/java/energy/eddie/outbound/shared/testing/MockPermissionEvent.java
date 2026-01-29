// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.shared.testing;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record MockPermissionEvent(String permissionId, PermissionProcessStatus status, ZonedDateTime eventCreated) implements PermissionEvent {
    public MockPermissionEvent(String permissionId, PermissionProcessStatus status) {
        this(permissionId, status, ZonedDateTime.now(ZoneOffset.UTC));
    }
}