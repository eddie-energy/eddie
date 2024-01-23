package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.RejectedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class RejectedState
        extends ContextualizedPermissionRequestState<EsPermissionRequest>
        implements RejectedPermissionRequestState {
    public RejectedState(EsPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}