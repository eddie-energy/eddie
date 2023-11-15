package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TerminalPermissionRequestState;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;

public class AiidaTerminatedPermissionRequestState extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements TerminalPermissionRequestState {
    protected AiidaTerminatedPermissionRequestState(AiidaPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public PermissionProcessStatus status() {
        return PermissionProcessStatus.TERMINATED;
    }
}