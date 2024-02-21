package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.states.MalformedPermissionRequestState;
import jakarta.annotation.Nullable;

/**
 * When a permission request cannot be successfully validated, it's state will be malformed.
 */
public class EnerginetCustomerMalformedState
        extends ContextualizedPermissionRequestState<PermissionRequest>
        implements MalformedPermissionRequestState {
    @Nullable
    private final Throwable cause;

    public EnerginetCustomerMalformedState(PermissionRequest permissionRequest, @Nullable Throwable cause) {
        super(permissionRequest);
        this.cause = cause;
    }

    @Override
    public String toString() {
        return "MalformedPermissionRequestState{" +
                "cause=" + cause +
                '}';
    }
}