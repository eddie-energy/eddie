// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.permission.request.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.util.UUID;

/**
 * Emitted when AIIDA reports that a permission request was rejected by the user or cannot be fulfilled, together with
 * an optional reason.
 */
@Entity(name = "AiidaStatusUpdateEvent")
public class AiidaStatusUpdateEvent extends AiidaIdReceivedEvent {
    @Nullable
    @Column(name = "message")
    private final String message;

    @SuppressWarnings("NullAway.Init") // Needed for JPA
    protected AiidaStatusUpdateEvent() {
        this.message = null;
    }

    public AiidaStatusUpdateEvent(
            String permissionId,
            PermissionProcessStatus status,
            UUID aiidaId,
            @Nullable String message
    ) {
        super(permissionId, status, aiidaId);
        this.message = message;
    }

    @Nullable
    public String message() {
        return message;
    }
}
