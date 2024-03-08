package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.TimedOutPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisTimedOutState extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest> implements TimedOutPermissionRequestState {
    public FrEnedisTimedOutState(FrEnedisPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
