package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.states.UnableToSendPermissionRequestState;

public class FrEnedisUnableToSendState
        extends ContextualizedPermissionRequestState<TimeframedPermissionRequest>
        implements UnableToSendPermissionRequestState {
    private final Throwable t;

    public FrEnedisUnableToSendState(TimeframedPermissionRequest permissionRequest, Throwable t) {
        super(permissionRequest);
        this.t = t;
    }

    @Override
    public String toString() {
        return "UnableToSendState{" +
                "t=" + t +
                '}';
    }
}