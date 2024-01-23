package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.UnableToSendPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

/**
 * The UnableToSendPermissionRequestState indicates that we were not able to send the permission request to the 3rd party service.
 * This state is recoverable, by retrying to send the permission request.
 */
public class AtUnableToSendPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements UnableToSendPermissionRequestState {
    private final Throwable cause;

    protected AtUnableToSendPermissionRequestState(AtPermissionRequest permissionRequest, Throwable cause) {
        super(permissionRequest);
        this.cause = cause;
    }

    @Override
    public String toString() {
        return "UnableToSendPermissionRequestState{" +
                "cause=" + cause +
                '}';
    }

}