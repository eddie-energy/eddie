package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public class AtSentToPermissionAdministratorPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements SentToPermissionAdministratorPermissionRequestState {

    public AtSentToPermissionAdministratorPermissionRequestState(AtPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void accept() {
        permissionRequest.changeState(new AtAcceptedPermissionRequestState(permissionRequest));
    }

    @Override
    public void invalid() {
        permissionRequest.changeState(new AtInvalidPermissionRequestState(permissionRequest));
    }

    @Override
    public void reject() {
        permissionRequest.changeState(new AtRejectedPermissionRequestState(permissionRequest));
    }

    @Override
    public void timeOut() {
        throw new IllegalStateException("Not implemented yet");
    }
}