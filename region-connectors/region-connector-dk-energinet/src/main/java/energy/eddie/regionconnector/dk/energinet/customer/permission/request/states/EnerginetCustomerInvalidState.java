package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.InvalidPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerInvalidState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements InvalidPermissionRequestState {
    public EnerginetCustomerInvalidState(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}