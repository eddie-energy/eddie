// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.requests.OAuthRequestType;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.ZonedDateTime;

@Entity(name = "CdsSentToPaEvent")
public class SentToPaEvent extends PersistablePermissionEvent   {
    @Column(name = "auth_expires_at")
    @Nullable
    @SuppressWarnings("unused")
    private final ZonedDateTime authExpiresAt;
    @Column(name = "state")
    private final String state;
    @Column(name = "oauth_request_type")
    @Enumerated(EnumType.STRING)
    private final OAuthRequestType oAuthRequestType;
    @Column(name = "redirect_uri")
    @Nullable
    private final String redirectUri;

    public SentToPaEvent(
            String permissionId,
            @Nullable ZonedDateTime authExpiresAt,
            String state,
            OAuthRequestType oAuthRequestType
    ) {
        super(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        this.authExpiresAt = authExpiresAt;
        this.state = state;
        this.oAuthRequestType = oAuthRequestType;
        this.redirectUri = null;
    }

    public SentToPaEvent(
            String permissionId,
            @Nullable ZonedDateTime authExpiresAt,
            String state,
            OAuthRequestType oAuthRequestType,
            @Nullable String redirectUri
    ) {
        super(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        this.authExpiresAt = authExpiresAt;
        this.state = state;
        this.oAuthRequestType = oAuthRequestType;
        this.redirectUri = redirectUri;
    }

    @SuppressWarnings("NullAway")
    protected SentToPaEvent() {
        super();
        authExpiresAt = null;
        state = null;
        oAuthRequestType = null;
        redirectUri = null;
    }

    public String state() {
        return state;
    }

    public boolean isPushedAuthorizationRequest() {
        return oAuthRequestType == OAuthRequestType.PUSHED_AUTHORIZATION_REQUEST;
    }

    @Nullable
    public String redirectUri() {
        return redirectUri;
    }
}
