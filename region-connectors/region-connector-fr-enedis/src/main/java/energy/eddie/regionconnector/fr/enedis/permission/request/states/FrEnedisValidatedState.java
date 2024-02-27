package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisValidatedState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements ValidatedPermissionRequestState {

    public FrEnedisValidatedState(FrEnedisPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void sendToPermissionAdministrator() {
        permissionRequest.changeState(new FrEnedisPendingAcknowledgmentState(permissionRequest));
    }
}