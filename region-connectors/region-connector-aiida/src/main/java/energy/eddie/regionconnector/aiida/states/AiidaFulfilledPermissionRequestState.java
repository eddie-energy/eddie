package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.FulfilledPermissionRequestState;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;

public class AiidaFulfilledPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements FulfilledPermissionRequestState {
    protected AiidaFulfilledPermissionRequestState(AiidaPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}