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
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.annotation.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
@ConditionalOnProperty(name = "region-connector.cds.par.enabled", matchIfMissing = true, havingValue = "false")
public class SimpleAuthorizationService implements AuthorizationService {
    private final CdsServerClientFactory factory;
    private final Outbox outbox;

    public SimpleAuthorizationService(CdsServerClientFactory factory, Outbox outbox) {
        this.factory = factory;
        this.outbox = outbox;
    }

    @Override
    @Nullable
    public URI createOAuthRequest(CdsServer cdsServer, String permissionId) {
        var authResult = factory.get(cdsServer).createAuthorizationUri(List.of(Scopes.CUSTOMER_DATA_SCOPE));
        if (authResult.isEmpty()) {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.MALFORMED));
            return null;
        }
        var res = authResult.get();
        outbox.commit(new SentToPaEvent(permissionId,
                                        null,
                                        res.state(),
                                        OAuthRequestType.IMPLICIT_GRANT_TYPE,
                                        res.redirectUri().toString()));
        return res.redirectUri();
    }
}
