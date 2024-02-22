package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.UnableToSendPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import jakarta.annotation.Nullable;

/**
 * The UnableToSendState indicates that we were not able to send the permission request to the 3rd party service.
 * This state is recoverable, by retrying to send the permission request.
 */
public class UnableToSendState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements UnableToSendPermissionRequestState {
    @Nullable
    private final Throwable reason;

    public UnableToSendState(EsPermissionRequest permissionRequest, @Nullable Throwable reason) {
        super(permissionRequest);
        this.reason = reason;
    }

    public @Nullable Throwable reason() {
        return reason;
    }

    @Override
    public String toString() {
        return "UnableToSendState{" +
                "reason=" + reason +
                '}';
    }
}