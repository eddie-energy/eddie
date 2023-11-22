package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.PendingAcknowledgmentPermissionRequestState;

public class FrEnedisPendingAcknowledgmentState extends ContextualizedPermissionRequestState<TimeframedPermissionRequest> implements PendingAcknowledgmentPermissionRequestState {
    protected FrEnedisPendingAcknowledgmentState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        permissionRequest.changeState(new FrEnedisSentToPermissionAdministratorState(permissionRequest));
    }
}