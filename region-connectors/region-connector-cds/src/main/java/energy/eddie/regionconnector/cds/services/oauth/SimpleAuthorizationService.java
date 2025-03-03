package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.cds.permission.requests.OAuthRequestType;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
@ConditionalOnProperty(name = "region-connector.cds.par.enabled", matchIfMissing = true, havingValue = "false")
public class SimpleAuthorizationService implements AuthorizationService {
    private final OAuthService oAuthService;
    private final Outbox outbox;

    public SimpleAuthorizationService(OAuthService oAuthService, Outbox outbox) {
        this.oAuthService = oAuthService;
        this.outbox = outbox;
    }

    @Override
    public URI createOAuthRequest(CdsServer cdsServer, String permissionId) {
        var res = oAuthService.createAuthorizationUri(cdsServer, List.of(Scopes.CUSTOMER_DATA_SCOPE));
        outbox.commit(new SentToPaEvent(permissionId, null, res.state(), OAuthRequestType.IMPLICIT_GRANT_TYPE));
        return res.redirectUri();
    }
}
