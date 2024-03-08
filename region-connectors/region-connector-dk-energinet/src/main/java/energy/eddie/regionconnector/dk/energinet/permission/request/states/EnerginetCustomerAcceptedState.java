package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerAcceptedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements AcceptedPermissionRequestState {
    private final StateBuilderFactory factory;

    public EnerginetCustomerAcceptedState(
            DkEnerginetCustomerPermissionRequest permissionRequest,
            StateBuilderFactory factory
    ) {
        super(permissionRequest);
        this.factory = factory;
    }

    @Override
    public void terminate() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.TERMINATED)
                        .build()
        );
    }

    @Override
    public void revoke() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.REVOKED)
                        .build()
        );
    }

    @Override
    public void fulfill() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.FULFILLED)
                        .build()
        );
    }
}
