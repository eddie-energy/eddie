package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.TerminalPermissionRequestState;

/**
 * The state of a permission request if it was malformed could not be validated.
 */
public class MalformedPermissionRequestState extends ContextualizedPermissionRequestState<PermissionRequest> implements TerminalPermissionRequestState {
    private final Throwable cause;

    public MalformedPermissionRequestState(PermissionRequest permissionRequest, Throwable cause) {
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
