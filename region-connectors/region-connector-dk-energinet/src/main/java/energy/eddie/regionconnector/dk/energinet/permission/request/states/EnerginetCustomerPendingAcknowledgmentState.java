package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.PendingAcknowledgmentPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;

public class EnerginetCustomerPendingAcknowledgmentState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements PendingAcknowledgmentPermissionRequestState {
    private final StateBuilderFactory factory;

    public EnerginetCustomerPendingAcknowledgmentState(DkEnerginetCustomerPermissionRequest permissionRequest, StateBuilderFactory factory) {
        super(permissionRequest);
        this.factory = factory;
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR).build()
        );
    }
}