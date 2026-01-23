// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class DkUnfulfillableEvent extends PersistablePermissionEvent {
    @Column(name = "errors", columnDefinition = "text")
    private final String reason;

    public DkUnfulfillableEvent(String permissionId, String reason) {
        super(permissionId, PermissionProcessStatus.UNFULFILLABLE);
        this.reason = reason;
    }

    protected DkUnfulfillableEvent() {
        reason = null;
    }
}
