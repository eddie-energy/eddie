package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.TerminalPermissionRequestState;

public class RejectedPermissionRequestState extends ContextualizedPermissionRequestState<PermissionRequest> implements TerminalPermissionRequestState {

    protected RejectedPermissionRequestState(PermissionRequest permissionRequest) {
        super(permissionRequest);
    }

}
