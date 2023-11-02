package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.RejectedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerRejectedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest> implements RejectedPermissionRequestState {
    public EnerginetCustomerRejectedState(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}