// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings("NullAway") // Needed for JPA
public class AcceptedEvent extends PersistablePermissionEvent {
    private final String meteringPointId;
    private final String cmConsentId;
    @Column(columnDefinition = "text")
    private final String message;

    public AcceptedEvent() {
        super();
        meteringPointId = null;
        cmConsentId = null;
        message = null;
    }

    public AcceptedEvent(String permissionId, String meteringPointId, String cmConsentId, String message) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.meteringPointId = meteringPointId;
        this.cmConsentId = cmConsentId;
        this.message = message;
    }

    public String meteringPointId() {
        return meteringPointId;
    }

    public String cmConsentId() {
        return cmConsentId;
    }

    public String message() {
        return message;
    }
}
