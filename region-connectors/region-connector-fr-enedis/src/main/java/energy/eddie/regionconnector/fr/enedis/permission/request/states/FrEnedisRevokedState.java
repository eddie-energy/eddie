package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.RevokedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

class FrEnedisRevokedState extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest> implements RevokedPermissionRequestState {
    protected FrEnedisRevokedState(FrEnedisPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}