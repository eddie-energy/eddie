package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;

public class AiidaTerminatedPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements TerminatedPermissionRequestState {
    protected AiidaTerminatedPermissionRequestState(AiidaPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}