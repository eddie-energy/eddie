package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.states.AcceptedPermissionRequestState;

public class FrEnedisAcceptedState
        extends ContextualizedPermissionRequestState<TimeframedPermissionRequest>
        implements AcceptedPermissionRequestState {

    public static final String NOT_IMPLEMENTED_YET = "Not implemented yet";

    public FrEnedisAcceptedState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void terminate() {
        permissionRequest.changeState(new FrEnedisTerminatedState(permissionRequest));
    }

    @Override
    public void revoke() {
        permissionRequest.changeState(new FrEnedisRevokedState(permissionRequest));
    }

    @Override
    public void fulfill() {
        permissionRequest.changeState(new FrEnedisFulfilledState(permissionRequest));
    }
}