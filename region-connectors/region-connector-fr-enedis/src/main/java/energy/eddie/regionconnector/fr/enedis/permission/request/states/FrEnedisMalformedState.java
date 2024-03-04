package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.api.agnostic.process.model.states.MalformedPermissionRequestState;
import jakarta.annotation.Nullable;


public class FrEnedisMalformedState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements MalformedPermissionRequestState {
    @Nullable
    private final Throwable cause;

    public FrEnedisMalformedState(FrEnedisPermissionRequest permissionRequest, @Nullable Throwable cause) {
        super(permissionRequest);
        this.cause = cause;
    }

    @Override
    public String toString() {
        return "MalformedState{" +
                "cause=" + cause +
                '}';
    }
}