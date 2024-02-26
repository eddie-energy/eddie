package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisTerminatedState extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest> implements TerminatedPermissionRequestState {
    public FrEnedisTerminatedState(FrEnedisPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}