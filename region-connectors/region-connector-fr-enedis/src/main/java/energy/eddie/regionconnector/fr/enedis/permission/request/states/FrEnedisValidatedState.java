package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.states.ValidatedPermissionRequestState;

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