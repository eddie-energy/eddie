package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import reactor.core.publisher.Mono;

public interface AuthorizationApi {
    Mono<AuthorizationRequestResponse> postAuthorizationRequest(AuthorizationRequest authorizationRequest);
}
