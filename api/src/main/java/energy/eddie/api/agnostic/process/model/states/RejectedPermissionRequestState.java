package energy.eddie.api.agnostic.process.model.states;

import energy.eddie.api.agnostic.process.model.TerminalPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;

public interface RejectedPermissionRequestState extends TerminalPermissionRequestState {
    @Override
    default PermissionProcessStatus status() {
        return PermissionProcessStatus.REJECTED;
    }
}