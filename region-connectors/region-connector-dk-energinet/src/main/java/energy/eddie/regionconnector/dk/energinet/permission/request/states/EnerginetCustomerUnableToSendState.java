package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.UnableToSendPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import jakarta.annotation.Nullable;


public class EnerginetCustomerUnableToSendState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements UnableToSendPermissionRequestState {
    @Nullable
    private final Throwable t;

    public EnerginetCustomerUnableToSendState(DkEnerginetCustomerPermissionRequest permissionRequest, @Nullable Throwable t) {
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