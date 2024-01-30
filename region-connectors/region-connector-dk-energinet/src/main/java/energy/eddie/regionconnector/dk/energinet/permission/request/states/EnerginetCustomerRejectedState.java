package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.RejectedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerRejectedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest> implements RejectedPermissionRequestState {
    public EnerginetCustomerRejectedState(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }
}