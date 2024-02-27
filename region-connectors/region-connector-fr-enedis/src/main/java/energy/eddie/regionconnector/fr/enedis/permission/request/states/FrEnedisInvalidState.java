package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.InvalidPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisInvalidState extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest> implements InvalidPermissionRequestState {
    public FrEnedisInvalidState(FrEnedisPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}