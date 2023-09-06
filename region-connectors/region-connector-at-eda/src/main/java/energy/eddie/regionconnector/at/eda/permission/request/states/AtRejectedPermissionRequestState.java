package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.states.RejectedPermissionRequestState;

public class AtRejectedPermissionRequestState
        extends ContextualizedPermissionRequestState<PermissionRequest>
        implements RejectedPermissionRequestState {

    protected AtRejectedPermissionRequestState(PermissionRequest permissionRequest) {
        super(permissionRequest);
    }

}
