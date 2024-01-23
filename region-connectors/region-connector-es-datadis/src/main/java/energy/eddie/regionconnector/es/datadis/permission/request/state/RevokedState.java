package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.RevokedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class RevokedState
        extends ContextualizedPermissionRequestState<EsPermissionRequest>
        implements RevokedPermissionRequestState {
    protected RevokedState(EsPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}