package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.RevokedPermissionRequestState;

class FrEnedisRevokedState extends ContextualizedPermissionRequestState<TimeframedPermissionRequest> implements RevokedPermissionRequestState {
    protected FrEnedisRevokedState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
