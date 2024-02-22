package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.InvalidPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import jakarta.annotation.Nullable;

public class InvalidState
        extends ContextualizedPermissionRequestState<EsPermissionRequest>
        implements InvalidPermissionRequestState {
    @Nullable
    private final Throwable reason;

    public InvalidState(EsPermissionRequest permissionRequest, @Nullable Throwable reason) {
        super(permissionRequest);
        this.reason = reason;
    }

    public @Nullable Throwable reason() {
        return reason;
    }

    @Override
    public String toString() {
        return "InvalidState{" +
                "reason=" + reason +
                '}';
    }
}