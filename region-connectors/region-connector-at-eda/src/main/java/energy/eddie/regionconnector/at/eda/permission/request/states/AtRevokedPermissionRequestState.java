package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.RevokedPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public class AtRevokedPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements RevokedPermissionRequestState {
    public AtRevokedPermissionRequestState(AtPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}