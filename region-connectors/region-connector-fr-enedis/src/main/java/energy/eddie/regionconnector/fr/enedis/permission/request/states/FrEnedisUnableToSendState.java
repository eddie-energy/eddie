package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.UnableToSendPermissionRequestState;
import jakarta.annotation.Nullable;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisUnableToSendState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements UnableToSendPermissionRequestState {
    @Nullable
    private final Throwable t;

    public FrEnedisUnableToSendState(FrEnedisPermissionRequest permissionRequest, @Nullable Throwable t) {
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