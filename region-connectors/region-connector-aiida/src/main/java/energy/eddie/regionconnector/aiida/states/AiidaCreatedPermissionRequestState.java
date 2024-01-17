package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;

public class AiidaCreatedPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements CreatedPermissionRequestState {

    public AiidaCreatedPermissionRequestState(AiidaPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void validate() {
        // request already contains only valid values because controller validates input
        permissionRequest.changeState(new AiidaValidatedPermissionRequestState(permissionRequest));
    }
}