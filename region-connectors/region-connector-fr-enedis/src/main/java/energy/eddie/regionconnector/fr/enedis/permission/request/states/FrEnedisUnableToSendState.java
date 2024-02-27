package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.UnableToSendPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisUnableToSendState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements UnableToSendPermissionRequestState {
    private final Throwable t;

    public FrEnedisUnableToSendState(FrEnedisPermissionRequest permissionRequest, Throwable t) {
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