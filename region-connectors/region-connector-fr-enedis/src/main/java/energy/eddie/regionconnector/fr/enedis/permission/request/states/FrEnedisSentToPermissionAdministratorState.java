package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisSentToPermissionAdministratorState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements SentToPermissionAdministratorPermissionRequestState {
    public FrEnedisSentToPermissionAdministratorState(FrEnedisPermissionRequest permissionRequest) {
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