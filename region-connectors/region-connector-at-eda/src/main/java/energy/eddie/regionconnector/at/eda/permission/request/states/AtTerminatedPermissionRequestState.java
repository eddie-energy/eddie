package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public class AtTerminatedPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements TerminatedPermissionRequestState {
    protected AtTerminatedPermissionRequestState(AtPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
