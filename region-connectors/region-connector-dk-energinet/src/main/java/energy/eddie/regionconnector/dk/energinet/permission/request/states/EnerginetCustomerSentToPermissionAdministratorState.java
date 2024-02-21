package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerSentToPermissionAdministratorState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements SentToPermissionAdministratorPermissionRequestState {
    private final StateBuilderFactory factory;

    public EnerginetCustomerSentToPermissionAdministratorState(DkEnerginetCustomerPermissionRequest permissionRequest, StateBuilderFactory factory) {
        super(permissionRequest);
        this.factory = factory;
    }

    @Override
    public void accept() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.ACCEPTED).build()
        );
    }

    @Override
    public void invalid() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.INVALID).build()
        );
    }

    @Override
    public void reject() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.REJECTED).build()
        );
    }

    @Override
    public void timeOut() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}