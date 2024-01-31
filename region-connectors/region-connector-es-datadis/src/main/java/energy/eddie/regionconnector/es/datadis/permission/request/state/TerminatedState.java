package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class TerminatedState extends ContextualizedPermissionRequestState<EsPermissionRequest>
        implements TerminatedPermissionRequestState {
    protected TerminatedState(EsPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
