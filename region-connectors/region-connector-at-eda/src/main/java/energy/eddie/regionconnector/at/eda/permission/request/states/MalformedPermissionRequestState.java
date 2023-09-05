package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.regionconnector.at.api.ContextualizedPermissionRequestState;
import energy.eddie.regionconnector.at.api.PermissionRequest;
import energy.eddie.regionconnector.at.api.TerminalPermissionRequestState;

/**
 * The state of a permission request if it was malformed could not be validated.
 */
public class MalformedPermissionRequestState extends ContextualizedPermissionRequestState implements TerminalPermissionRequestState {
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
