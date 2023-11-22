package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.SentToPermissionAdministratorPermissionRequestState;

public class FrEnedisSentToPermissionAdministratorState
        extends ContextualizedPermissionRequestState<TimeframedPermissionRequest>
        implements SentToPermissionAdministratorPermissionRequestState {
    public FrEnedisSentToPermissionAdministratorState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void accept() {
        permissionRequest.changeState(new FrEnedisAcceptedState(permissionRequest));
    }

    @Override
    public void invalid() {
        permissionRequest.changeState(new FrEnedisInvalidState(permissionRequest));
    }

    @Override
    public void reject() {
        permissionRequest.changeState(new FrEnedisRejectedState(permissionRequest));
    }


    @Override
    public void timeOut() {
        throw new IllegalStateException("Not implemented yet");
    }
}