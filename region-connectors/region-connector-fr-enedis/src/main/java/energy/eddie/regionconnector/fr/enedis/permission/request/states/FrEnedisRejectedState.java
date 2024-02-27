package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.RejectedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisRejectedState extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest> implements RejectedPermissionRequestState {
    public FrEnedisRejectedState(FrEnedisPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}