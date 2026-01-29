// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class EsSentToPermissionAdministratorEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String response;

    public EsSentToPermissionAdministratorEvent(String permissionId, String response) {
        super(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        this.response = response;
    }

    protected EsSentToPermissionAdministratorEvent() {
        super();
        response = null;
    }

    public AuthorizationRequestResponse response() {
        return AuthorizationRequestResponse.fromResponse(response);
    }
}
