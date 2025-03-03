package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.regionconnector.cds.master.data.CdsServer;
import reactor.core.publisher.Mono;

import java.net.URI;

public interface AuthorizationService {
    Mono<URI> createOAuthRequest(CdsServer cdsServer, String permissionId);
}
