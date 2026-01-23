// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity(name = "AiidaFailedToTerminateEvent")
public class FailedToTerminateEvent extends PersistablePermissionEvent {
    @Nullable
    @Column(name = "message")
    private final String message;

    @SuppressWarnings("NullAway.Init") // Needed for JPA
    protected FailedToTerminateEvent() {
        this.message = null;
    }

    public FailedToTerminateEvent(String permissionId, @Nullable String message) {
        super(permissionId, PermissionProcessStatus.FAILED_TO_TERMINATE);
        this.message = message;
    }

    @Nullable
    public String message() {
        return message;
    }
}
