package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;

public interface AuthorizationResponseHandler {
    void handleAuthorizationRequestResponse(String permissionId, AuthorizationRequestResponse response);
}
