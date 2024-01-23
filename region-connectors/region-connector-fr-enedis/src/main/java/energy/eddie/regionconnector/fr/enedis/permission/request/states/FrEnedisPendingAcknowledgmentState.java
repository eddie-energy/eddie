package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.states.PendingAcknowledgmentPermissionRequestState;

public class FrEnedisPendingAcknowledgmentState extends ContextualizedPermissionRequestState<TimeframedPermissionRequest> implements PendingAcknowledgmentPermissionRequestState {
    public FrEnedisPendingAcknowledgmentState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        permissionRequest.changeState(new FrEnedisSentToPermissionAdministratorState(permissionRequest));
    }
}