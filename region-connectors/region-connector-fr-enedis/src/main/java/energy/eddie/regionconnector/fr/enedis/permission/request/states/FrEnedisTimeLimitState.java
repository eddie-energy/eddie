package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.TimeLimitPermissionRequestState;

public class FrEnedisTimeLimitState extends ContextualizedPermissionRequestState<TimeframedPermissionRequest> implements TimeLimitPermissionRequestState {

    protected FrEnedisTimeLimitState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
