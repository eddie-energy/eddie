package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class ValidatedState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements ValidatedPermissionRequestState {
    private final AuthorizationRequest authorizationRequest;
    private final AuthorizationApi authorizationApi;

    public ValidatedState(EsPermissionRequest permissionRequest,
                          AuthorizationRequest authorizationRequest,
                          AuthorizationApi authorizationApi) {
        super(permissionRequest);
        this.authorizationRequest = authorizationRequest;
        this.authorizationApi = authorizationApi;
    }

    @Override
    public void sendToPermissionAdministrator() {
        try {
            AuthorizationRequestResponse response = authorizationApi.postAuthorizationRequest(authorizationRequest).block();
            permissionRequest.changeState(new PendingAcknowledgementState(permissionRequest, response));
        } catch (RuntimeException e) {
            permissionRequest.changeState(new UnableToSendState(permissionRequest, e));
        }
    }
}