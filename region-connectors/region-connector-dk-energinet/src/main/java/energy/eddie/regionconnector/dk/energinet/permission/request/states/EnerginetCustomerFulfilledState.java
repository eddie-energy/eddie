package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.FulfilledPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerFulfilledState
        extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements FulfilledPermissionRequestState {
    public EnerginetCustomerFulfilledState(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
