package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.RevokedPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

class AtRevokedPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements RevokedPermissionRequestState {
    protected AtRevokedPermissionRequestState(AtPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
