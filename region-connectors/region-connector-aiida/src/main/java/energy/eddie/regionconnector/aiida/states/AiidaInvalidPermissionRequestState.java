package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.states.InvalidPermissionRequestState;

public class AiidaInvalidPermissionRequestState
        extends ContextualizedPermissionRequestState<PermissionRequest>
        implements InvalidPermissionRequestState {
    public AiidaInvalidPermissionRequestState(PermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
