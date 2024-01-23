package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.states.InvalidPermissionRequestState;

public class AiidaInvalidPermissionRequestState
        extends ContextualizedPermissionRequestState<PermissionRequest>
        implements InvalidPermissionRequestState {
    public AiidaInvalidPermissionRequestState(PermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}