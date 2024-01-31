package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.MalformedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

/**
 * The state of a permission request if it was malformed could not be validated.
 */
public class MalformedState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements MalformedPermissionRequestState {
    private final Throwable reason;

    protected MalformedState(EsPermissionRequest permissionRequest, Throwable reason) {
        super(permissionRequest);
        this.reason = reason;
    }

    public Throwable reason() {
        return reason;
    }

    @Override
    public String toString() {
        return "MalformedPermissionRequestState{" +
                "reason=" + reason +
                '}';
    }
}