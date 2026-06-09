// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.persistence.Entity;

/**
 * Event emitted when a permission request is accepted.
 */
@Entity(name = "DeAcceptedEvent")
@SuppressWarnings({ "NullAway", "unused" })
public class AcceptedEvent extends PersistablePermissionEvent {

    public AcceptedEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
    }

    protected AcceptedEvent() {
    }
}
