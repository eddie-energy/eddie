// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.oauth.enums.OAuthErrorResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class UsInvalidEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    @Enumerated(EnumType.STRING)
    private final OAuthErrorResponse invalidReason;

    public UsInvalidEvent(String permissionId, OAuthErrorResponse rejectReason) {
        super(permissionId, PermissionProcessStatus.INVALID);
        this.invalidReason = rejectReason;
    }

    protected UsInvalidEvent() {
        super();
        this.invalidReason = null;
    }

    public OAuthErrorResponse invalidReason() {
        return invalidReason;
    }
}
