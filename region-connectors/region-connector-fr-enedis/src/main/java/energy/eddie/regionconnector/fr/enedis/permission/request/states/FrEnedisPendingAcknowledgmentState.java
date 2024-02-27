package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.PendingAcknowledgmentPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisPendingAcknowledgmentState extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest> implements PendingAcknowledgmentPermissionRequestState {
    public FrEnedisPendingAcknowledgmentState(FrEnedisPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        permissionRequest.changeState(new FrEnedisSentToPermissionAdministratorState(permissionRequest));
    }
}