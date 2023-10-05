package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerSentToPermissionAdministratorState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements SentToPermissionAdministratorPermissionRequestState {
    public EnerginetCustomerSentToPermissionAdministratorState(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void accept() {
        permissionRequest.changeState(new EnerginetCustomerAcceptedState(permissionRequest));
    }

    @Override
    public void invalid() {
        permissionRequest.changeState(new EnerginetCustomerInvalidState(permissionRequest));
    }

    @Override
    public void reject() {
        permissionRequest.changeState(new EnerginetCustomerRejectedState(permissionRequest));
    }
}
