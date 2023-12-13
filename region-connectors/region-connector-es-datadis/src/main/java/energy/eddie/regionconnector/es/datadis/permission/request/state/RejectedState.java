package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.RejectedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class RejectedState
        extends ContextualizedPermissionRequestState<EsPermissionRequest>
        implements RejectedPermissionRequestState {

    protected RejectedState(EsPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

}
