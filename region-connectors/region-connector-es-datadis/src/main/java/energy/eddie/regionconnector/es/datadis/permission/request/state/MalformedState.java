package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.MalformedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

/**
 * The state of a permission request if it was malformed could not be validated.
 */
public class MalformedState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements MalformedPermissionRequestState {
    private final Throwable cause;

    public MalformedState(EsPermissionRequest permissionRequest, Throwable cause) {
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
