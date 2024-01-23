package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public class AtAcceptedPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements AcceptedPermissionRequestState {

    public AtAcceptedPermissionRequestState(AtPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void terminate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revoke() {
        permissionRequest.changeState(new AtRevokedPermissionRequestState(permissionRequest));
    }

    @Override
    public void fulfill() {
        throw new IllegalStateException("Not implemented yet");
    }
}