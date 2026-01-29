// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
public class AiidaIdReceivedEvent extends PersistablePermissionEvent {
    @Column(name = "aiida_id", nullable = false)
    @SuppressWarnings("unused")
    private final UUID aiidaId;

    @SuppressWarnings("NullAway")
    protected AiidaIdReceivedEvent() {
        this.aiidaId = null;
    }

    public AiidaIdReceivedEvent(String permissionId, PermissionProcessStatus status, UUID aiidaId) {
        super(permissionId, status);
        this.aiidaId = aiidaId;
    }
}
