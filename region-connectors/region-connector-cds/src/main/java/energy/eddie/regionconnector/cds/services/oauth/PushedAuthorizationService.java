// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.requests.OAuthRequestType;
import energy.eddie.regionconnector.cds.services.oauth.par.ErrorParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.SuccessfulParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.UnableToSendPar;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "region-connector.cds.par.enabled", havingValue = "true")
public class PushedAuthorizationService implements AuthorizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushedAuthorizationService.class);
    private final CdsServerClientFactory factory;
    private final Outbox outbox;

    public PushedAuthorizationService(CdsServerClientFactory factory, Outbox outbox) {
        this.factory = factory;
        this.outbox = outbox;
    }

    @Override
    public URI createOAuthRequest(CdsServer cdsServer, String permissionId) {
        var res = factory.get(cdsServer).pushAuthorizationRequest(List.of(Scopes.USAGE_DETAILED_SCOPE));
        return switch (res) {
            case ErrorParResponse(String code) -> {
                LOGGER.info("Got error when requesting PAR '{}' for permission request {}",
                            code,
                            permissionId);
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
                yield null;
            }
            case UnableToSendPar ignored -> {
                LOGGER.info("Was not able to send permission request {}", permissionId);
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
                yield null;
            }
            case SuccessfulParResponse(URI redirectUri, ZonedDateTime expiresAt, String state) -> {
                outbox.commit(new SentToPaEvent(permissionId,
                                                expiresAt,
                                                state,
                                                OAuthRequestType.PUSHED_AUTHORIZATION_REQUEST,
                                                redirectUri.toString()));
                yield redirectUri;
            }
        };
    }
}
