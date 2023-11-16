package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.PendingAcknowledgmentPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerPendingAcknowledgmentState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements PendingAcknowledgmentPermissionRequestState {
    protected EnerginetCustomerPendingAcknowledgmentState(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        permissionRequest.changeState(new EnerginetCustomerSentToPermissionAdministratorState(permissionRequest));
    }
}
