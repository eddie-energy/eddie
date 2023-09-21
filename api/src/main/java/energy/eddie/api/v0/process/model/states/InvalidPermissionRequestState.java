package energy.eddie.api.v0.process.model.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.TerminalPermissionRequestState;

public interface InvalidPermissionRequestState extends TerminalPermissionRequestState {
    @Override
    default PermissionProcessStatus status() {
        return PermissionProcessStatus.INVALID;
    }
}
