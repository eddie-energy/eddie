package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.RevokedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisRevokedState extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest> implements RevokedPermissionRequestState {
    public FrEnedisRevokedState(FrEnedisPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}