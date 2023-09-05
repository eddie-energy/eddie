package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.TerminalPermissionRequestState;

public class InvalidPermissionRequestState extends ContextualizedPermissionRequestState<PermissionRequest> implements TerminalPermissionRequestState {
    public InvalidPermissionRequestState(PermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
