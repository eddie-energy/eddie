// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class DkAcceptedEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String accessToken;

    public DkAcceptedEvent(String permissionId, String accessToken) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.accessToken = accessToken;
    }

    protected DkAcceptedEvent() {
        accessToken = null;
    }

    public String accessToken() {
        return accessToken;
    }
}
