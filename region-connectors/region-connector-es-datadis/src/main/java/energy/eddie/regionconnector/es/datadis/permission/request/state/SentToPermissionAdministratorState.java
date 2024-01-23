package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class SentToPermissionAdministratorState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements SentToPermissionAdministratorPermissionRequestState {

    protected SentToPermissionAdministratorState(EsPermissionRequest permissionRequest, AuthorizationRequestResponse response) {
        super(permissionRequest);

        if (response == AuthorizationRequestResponse.NO_NIF) {
            permissionRequest.changeState(new InvalidState(permissionRequest, new Throwable("Given NIF does not exist")));
        } else if (response == AuthorizationRequestResponse.NO_SUPPLIES) {
            permissionRequest.changeState(new InvalidState(permissionRequest, new Throwable("The given NIF has no associated supplies")));
        }
    }

    @Override
    public void accept() {
        permissionRequest.changeState(new AcceptedState(permissionRequest));
    }

    @Override
    public void invalid() {
        permissionRequest.changeState(new InvalidState(permissionRequest, new Throwable("Invalid permission request")));
    }

    @Override
    public void reject() {
        permissionRequest.changeState(new RejectedState(permissionRequest));
    }

    @Override
    public void timeOut() {
        throw new IllegalStateException("Not implemented yet");
    }
}