// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings("NullAway") // Needed for JPA
public class TerminationEvent extends PersistablePermissionEvent {
    private final String message;

    public TerminationEvent(String permissionId, String message) {
        super(permissionId, PermissionProcessStatus.TERMINATED);
        this.message = message;
    }

    protected TerminationEvent() {
        super();
        this.message = null;
    }

    public String message() {
        return message;
    }
}
