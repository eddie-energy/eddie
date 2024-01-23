package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.RevokedPermissionRequestState;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;

public class AiidaRevokedPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements RevokedPermissionRequestState {
    protected AiidaRevokedPermissionRequestState(AiidaPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}