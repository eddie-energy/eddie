package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.RevokedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerRevokedState
        extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements RevokedPermissionRequestState {
    protected EnerginetCustomerRevokedState(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}