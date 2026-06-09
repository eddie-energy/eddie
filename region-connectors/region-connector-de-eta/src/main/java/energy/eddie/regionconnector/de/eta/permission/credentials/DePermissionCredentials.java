// SPDX-FileCopyrightText: 2026 The ETA+ Developers <bilal.sakhawat@etaplus.energy>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.permission.credentials;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "DePermissionCredentials")
@Table(schema = "de_eta", name = "permission_credentials")
@SuppressWarnings("NullAway")
public class DePermissionCredentials {

    @Id
    @Column(name = "permission_id", length = 36, nullable = false)
    private String permissionId;

    @Column(name = "access_token", columnDefinition = "text", nullable = false)
    private String accessToken;

    @Nullable
    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken;

    public DePermissionCredentials(String permissionId, String accessToken, @Nullable String refreshToken) {
        this.permissionId = permissionId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    protected DePermissionCredentials() {
        this.permissionId = null;
        this.accessToken = null;
        this.refreshToken = null;
    }

    public String permissionId() {
        return permissionId;
    }

    public String accessToken() {
        return accessToken;
    }

    @Nullable
    public String refreshToken() {
        return refreshToken;
    }
}