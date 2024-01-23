package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;

public class AiidaAcceptedPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements AcceptedPermissionRequestState {
    public AiidaAcceptedPermissionRequestState(AiidaPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void terminate() {
        permissionRequest.changeState(new AiidaTerminatedPermissionRequestState(permissionRequest));
    }

    @Override
    public void revoke() {
        permissionRequest.changeState(new AiidaRevokedPermissionRequestState(permissionRequest));
    }

    @Override
    public void fulfill() {
        permissionRequest.changeState(new AiidaFulfilledPermissionRequestState(permissionRequest));
    }
}