package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class ValidatedState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements ValidatedPermissionRequestState {
    private final AuthorizationRequest authorizationRequest;
    private final AuthorizationApi authorizationApi;
    private final AuthorizationResponseHandler callback;

    protected ValidatedState(EsPermissionRequest permissionRequest,
                             AuthorizationRequest authorizationRequest,
                             AuthorizationApi authorizationApi,
                             AuthorizationResponseHandler callback) {
        super(permissionRequest);
        this.authorizationRequest = authorizationRequest;
        this.authorizationApi = authorizationApi;
        this.callback = callback;
    }

    @Override
    public void sendToPermissionAdministrator() {
        var response = authorizationApi
                .postAuthorizationRequest(authorizationRequest)
                .doOnSuccess(this::processResponse)
                // TODO should be changed via a transition method with #296
                .doOnError(error -> permissionRequest.changeState(new UnableToSendState(permissionRequest, error)));

        // TODO this will block the controller from sending a response until this returns
        var pendingState = new PendingAcknowledgementState(permissionRequest);
        permissionRequest.changeState(pendingState);
        response.subscribe();
    }


    private void processResponse(AuthorizationRequestResponse response) {
        callback.handleAuthorizationRequestResponse(permissionRequest.permissionId(), response);
    }
}
