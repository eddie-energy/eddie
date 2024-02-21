package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerTerminatedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements TerminatedPermissionRequestState {
    public EnerginetCustomerTerminatedState(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
