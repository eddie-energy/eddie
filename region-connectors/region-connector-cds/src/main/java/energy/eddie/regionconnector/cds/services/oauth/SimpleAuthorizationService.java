package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClientFactory;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.cds.permission.requests.OAuthRequestType;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@Service
@ConditionalOnProperty(name = "region-connector.cds.par.enabled", matchIfMissing = true, havingValue = "false")
public class SimpleAuthorizationService implements AuthorizationService {
    private final OAuthService oAuthService;
    private final CustomerDataClientFactory factory;
    private final Outbox outbox;

    public SimpleAuthorizationService(OAuthService oAuthService, CustomerDataClientFactory factory, Outbox outbox) {this.oAuthService = oAuthService;
        this.factory = factory;
        this.outbox = outbox;
    }

    @Override
    public Mono<URI> createOAuthRequest(CdsServer cdsServer, String permissionId) {
        return factory.create(cdsServer.id())
                .map(creds -> {
                    var res = oAuthService.createAuthorizationUri(cdsServer, creds, List.of(Scopes.CUSTOMER_DATA_SCOPE));
                    outbox.commit(new SentToPaEvent(permissionId, null, res.state(), OAuthRequestType.IMPLICIT_GRANT_TYPE));
                    return res.redirectUri();
                });
    }
}
