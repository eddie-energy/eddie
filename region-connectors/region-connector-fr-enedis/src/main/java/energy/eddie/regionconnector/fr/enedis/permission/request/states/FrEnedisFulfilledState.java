package energy.eddie.regionconnector.fr.enedis.permission.request.states;


import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.states.FulfilledPermissionRequestState;

public class FrEnedisFulfilledState extends ContextualizedPermissionRequestState<TimeframedPermissionRequest> implements FulfilledPermissionRequestState {

    protected FrEnedisFulfilledState(TimeframedPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
