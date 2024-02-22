package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.TimedOutPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class TimedOutState extends ContextualizedPermissionRequestState<EsPermissionRequest>
        implements TimedOutPermissionRequestState {
    public TimedOutState(EsPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}