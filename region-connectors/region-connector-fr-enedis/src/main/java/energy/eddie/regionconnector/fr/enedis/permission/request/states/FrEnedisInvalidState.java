package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.InvalidPermissionRequestState;

public class FrEnedisInvalidState extends ContextualizedPermissionRequestState<TimeframedPermissionRequest> implements InvalidPermissionRequestState {
    public FrEnedisInvalidState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}