package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.FulfilledPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisFulfilledState extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest> implements FulfilledPermissionRequestState {
    public FrEnedisFulfilledState(FrEnedisPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
