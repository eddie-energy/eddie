package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;

public class FrEnedisValidatedState
        extends ContextualizedPermissionRequestState<TimeframedPermissionRequest>
        implements ValidatedPermissionRequestState {

    public FrEnedisValidatedState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void sendToPermissionAdministrator() {
        permissionRequest.changeState(new FrEnedisPendingAcknowledgmentState(permissionRequest));
    }
}