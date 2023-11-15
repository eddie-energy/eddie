package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;

public class AiidaSentToPermissionAdministratorPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements SentToPermissionAdministratorPermissionRequestState {
    public AiidaSentToPermissionAdministratorPermissionRequestState(AiidaPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void accept() {
        permissionRequest.changeState(new AiidaAcceptedPermissionRequestState(permissionRequest));
    }

    @Override
    public void invalid() {
        permissionRequest.changeState(new AiidaInvalidPermissionRequestState(permissionRequest));
    }

    @Override
    public void reject() {
        throw new UnsupportedOperationException("AIIDA doesn't send rejected status messages to EDDIE, so this state is not supported");
    }

    @Override
    public void timeOut() {
        throw new UnsupportedOperationException("This region-connector doesn't support timeout for permission requests");
    }
}