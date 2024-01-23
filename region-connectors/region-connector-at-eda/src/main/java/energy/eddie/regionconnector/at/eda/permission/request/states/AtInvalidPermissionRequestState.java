package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.states.InvalidPermissionRequestState;

public class AtInvalidPermissionRequestState
        extends ContextualizedPermissionRequestState<PermissionRequest>
        implements InvalidPermissionRequestState {
    public AtInvalidPermissionRequestState(PermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}