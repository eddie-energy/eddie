package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.UnableToSendPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;


public class EnerginetCustomerUnableToSendState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements UnableToSendPermissionRequestState {
    private final Throwable t;

    public EnerginetCustomerUnableToSendState(DkEnerginetCustomerPermissionRequest permissionRequest, Throwable t) {
        super(permissionRequest);
        this.t = t;
    }

    @Override
    public String toString() {
        return "UnableToSendState{" +
                "t=" + t +
                '}';
    }
}