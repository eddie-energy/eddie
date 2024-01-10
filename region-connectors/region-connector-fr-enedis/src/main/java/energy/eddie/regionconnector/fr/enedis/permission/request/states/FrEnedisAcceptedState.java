package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.AcceptedPermissionRequestState;

public class FrEnedisAcceptedState
        extends ContextualizedPermissionRequestState<TimeframedPermissionRequest>
        implements AcceptedPermissionRequestState {

    public static final String NOT_IMPLEMENTED_YET = "Not implemented yet";

    public FrEnedisAcceptedState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void terminate() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    @Override
    public void revoke() {
        permissionRequest.changeState(new FrEnedisRevokedState(permissionRequest));
    }

    @Override
    public void timeLimit() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }
}