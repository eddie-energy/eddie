package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;

public class FrEnedisTerminatedState extends ContextualizedPermissionRequestState<TimeframedPermissionRequest> implements TerminatedPermissionRequestState {
    protected FrEnedisTerminatedState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
