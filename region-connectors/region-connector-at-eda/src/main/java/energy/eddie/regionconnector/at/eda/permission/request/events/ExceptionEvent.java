// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings("NullAway") // Needed for JPA
public class ExceptionEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String cause;

    public ExceptionEvent(String permissionId, PermissionProcessStatus status, Exception cause) {
        super(permissionId, status);
        this.cause = cause.getMessage();
    }

    public ExceptionEvent() {
        super();
        this.cause = null;
    }

    public String cause() {
        return cause;
    }
}