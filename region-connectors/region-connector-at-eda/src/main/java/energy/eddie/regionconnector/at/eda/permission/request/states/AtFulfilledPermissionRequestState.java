package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.FulfilledPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

class AtFulfilledPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements FulfilledPermissionRequestState {
    protected AtFulfilledPermissionRequestState(AtPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}