package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.states.RejectedPermissionRequestState;

public class FrEnedisRejectedState extends ContextualizedPermissionRequestState<TimeframedPermissionRequest> implements RejectedPermissionRequestState {
    public FrEnedisRejectedState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}