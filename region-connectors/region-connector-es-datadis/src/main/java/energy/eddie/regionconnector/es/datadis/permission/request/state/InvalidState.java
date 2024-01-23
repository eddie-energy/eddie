package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.InvalidPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class InvalidState
        extends ContextualizedPermissionRequestState<EsPermissionRequest>
        implements InvalidPermissionRequestState {

    Throwable reason;

    public InvalidState(EsPermissionRequest permissionRequest, Throwable reason) {
        super(permissionRequest);
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "InvalidState{" +
                "reason='" + reason + '\'' +
                '}';
    }
}