package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.regionconnector.at.api.ContextualizedPermissionRequestState;
import energy.eddie.regionconnector.at.api.PermissionRequest;
import energy.eddie.regionconnector.at.api.TerminalPermissionRequestState;

public class RejectedPermissionRequestState extends ContextualizedPermissionRequestState implements TerminalPermissionRequestState {

    protected RejectedPermissionRequestState(PermissionRequest permissionRequest) {
        super(permissionRequest);
    }

}
