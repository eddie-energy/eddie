package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;

public class AiidaValidatedPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements ValidatedPermissionRequestState {

    protected AiidaValidatedPermissionRequestState(AiidaPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void sendToPermissionAdministrator() {
        // AIIDA region connector skips the PendingAcknowledgmentPermissionRequestState, as there is no acknowledgement from AIIDA
        permissionRequest.changeState(new AiidaSentToPermissionAdministratorPermissionRequestState(permissionRequest));
    }
}