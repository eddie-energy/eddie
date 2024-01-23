package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

/**
 * The user has accepted the permission request
 */
public class AcceptedState
        extends ContextualizedPermissionRequestState<EsPermissionRequest>
        implements AcceptedPermissionRequestState {

    public AcceptedState(
            EsPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void terminate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revoke() {
        permissionRequest.changeState(new RevokedState(permissionRequest));
    }

    @Override
    public void fulfill() {
        throw new IllegalStateException("Not implemented yet");
    }
}