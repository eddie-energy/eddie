// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.permission.request.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
public class MqttCredentialsCreatedEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    public MqttCredentialsCreatedEvent(String permissionId) {
        // as this is an internal event, just repeat the previous PermissionProcessStatus
        super(permissionId, PermissionProcessStatus.ACCEPTED);
    }

    protected MqttCredentialsCreatedEvent() {
    }
}
