// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Event emitted when a permission request is accepted (OAuth token obtained).
 */
@Entity(name = "DeAcceptedEvent")
@SuppressWarnings({ "NullAway", "unused" })
public class AcceptedEvent extends PersistablePermissionEvent {

    @Column(name = "access_token", columnDefinition = "text")
    private String accessToken;

    public AcceptedEvent(String permissionId, String accessToken) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.accessToken = accessToken;
    }

    protected AcceptedEvent() {
    }

    public String accessToken() {
        return accessToken;
    }
}
