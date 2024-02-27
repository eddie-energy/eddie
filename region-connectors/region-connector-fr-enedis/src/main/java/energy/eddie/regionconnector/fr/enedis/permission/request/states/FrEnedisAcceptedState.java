package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisAcceptedState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements AcceptedPermissionRequestState {

    public static final String NOT_IMPLEMENTED_YET = "Not implemented yet";

    public FrEnedisAcceptedState(FrEnedisPermissionRequest permissionRequest) {
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
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }
}