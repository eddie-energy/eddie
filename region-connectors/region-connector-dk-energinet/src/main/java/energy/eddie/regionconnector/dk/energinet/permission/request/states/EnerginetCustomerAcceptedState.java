package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerAcceptedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements AcceptedPermissionRequestState {
    public EnerginetCustomerAcceptedState(DkEnerginetCustomerPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void terminate() {
        permissionRequest.changeState(new EnerginetCustomerTerminatedState(permissionRequest));
    }

    @Override
    public void revoke() {
        permissionRequest.changeState(new EnerginetCustomerRevokedState(permissionRequest));
    }

    @Override
    public void fulfill() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}