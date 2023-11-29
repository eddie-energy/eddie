package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;

public class AiidaTerminatedPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements TerminatedPermissionRequestState {
    protected AiidaTerminatedPermissionRequestState(AiidaPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}