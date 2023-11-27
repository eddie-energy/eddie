package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerAcceptedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements AcceptedPermissionRequestState {
    public EnerginetCustomerAcceptedState(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void terminate() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void revoke() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void timeLimit() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}