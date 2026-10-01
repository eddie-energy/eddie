// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings("NullAway") // Needed for JPA
public class TerminationEvent extends PersistablePermissionEvent {
    private final String conversationId;

    public TerminationEvent(String permissionId, String conversationId) {
        super(permissionId, PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION);
        this.conversationId = conversationId;
    }

    protected TerminationEvent() {
        super();
        this.conversationId = null;
    }

    public String conversationId() {
        return conversationId;
    }
}
