package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import reactor.core.publisher.Mono;

public interface AuthorizationApi {
    Mono<Void> postAuthorizationRequest(AuthorizationRequest authorizationRequest);
}
