// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@SuppressWarnings("NullAway")
@Entity(name = "BeInvalidEvent")
public class InvalidEvent extends PersistablePermissionEvent {
    @SuppressWarnings("unused")
    @Column(columnDefinition = "text", name = "invalid_message")
    private final String invalidMessage;

    public InvalidEvent(String permissionId, String invalidMessage) {
        super(permissionId, PermissionProcessStatus.INVALID);
        this.invalidMessage = invalidMessage;
    }

    protected InvalidEvent() {
        invalidMessage = null;
    }
}
