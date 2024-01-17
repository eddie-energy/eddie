package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.TimeLimitPermissionRequestState;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;

public class AiidaTimeLimitPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements TimeLimitPermissionRequestState {
    protected AiidaTimeLimitPermissionRequestState(AiidaPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
