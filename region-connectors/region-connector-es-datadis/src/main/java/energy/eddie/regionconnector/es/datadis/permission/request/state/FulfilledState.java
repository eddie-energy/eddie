package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.FulfilledPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class FulfilledState
        extends ContextualizedPermissionRequestState<EsPermissionRequest>
        implements FulfilledPermissionRequestState {
    protected FulfilledState(EsPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}