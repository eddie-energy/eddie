package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class SentToPermissionAdministratorState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements SentToPermissionAdministratorPermissionRequestState {

    protected SentToPermissionAdministratorState(EsPermissionRequest permissionRequest) {
        super(permissionRequest);
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
}
