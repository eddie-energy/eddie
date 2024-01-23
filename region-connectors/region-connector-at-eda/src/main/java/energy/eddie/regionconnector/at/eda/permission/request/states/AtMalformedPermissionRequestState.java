package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.states.MalformedPermissionRequestState;

/**
 * The state of a permission request if it was malformed could not be validated.
 */
public class AtMalformedPermissionRequestState
        extends ContextualizedPermissionRequestState<PermissionRequest>
        implements MalformedPermissionRequestState {
    private final Throwable cause;

    public AtMalformedPermissionRequestState(PermissionRequest permissionRequest, Throwable cause) {
        super(permissionRequest);
        this.cause = cause;
    }

    @Override
    public String toString() {
        return "MalformedPermissionRequestState{" +
                "cause=" + cause +
                '}';
    }
}