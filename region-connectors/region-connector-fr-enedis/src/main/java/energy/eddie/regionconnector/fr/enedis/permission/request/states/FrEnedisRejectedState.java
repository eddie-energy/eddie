package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.RejectedPermissionRequestState;

public class FrEnedisRejectedState extends ContextualizedPermissionRequestState<TimeframedPermissionRequest> implements RejectedPermissionRequestState {
    public FrEnedisRejectedState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}