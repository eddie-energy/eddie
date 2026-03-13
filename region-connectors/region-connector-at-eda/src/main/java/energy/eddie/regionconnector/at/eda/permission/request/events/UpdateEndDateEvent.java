// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
public class UpdateEndDateEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    private final LocalDate permissionEnd;

    public UpdateEndDateEvent(String permissionId, LocalDate permissionEnd) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.permissionEnd = permissionEnd;
    }

    @SuppressWarnings("NullAway")
    protected UpdateEndDateEvent() {
        this.permissionEnd = null;
    }

    public LocalDate permissionEnd() {
        return permissionEnd;
    }
}
