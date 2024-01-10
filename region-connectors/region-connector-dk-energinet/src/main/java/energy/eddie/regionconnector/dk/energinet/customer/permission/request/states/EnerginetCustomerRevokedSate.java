package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.RevokedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerRevokedSate
        extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements RevokedPermissionRequestState {
    protected EnerginetCustomerRevokedSate(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}
