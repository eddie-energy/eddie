package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.UnableToSendPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

/**
 * The UnableToSendState indicates that we were not able to send the permission request to the 3rd party service.
 * This state is recoverable, by retrying to send the permission request.
 */
public class UnableToSendState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements UnableToSendPermissionRequestState {
    private final Throwable cause;

    protected UnableToSendState(EsPermissionRequest permissionRequest, Throwable cause) {
        super(permissionRequest);
        this.cause = cause;
    }

    @Override
    public String toString() {
        return "UnableToSendPermissionRequestState{" +
                "cause=" + cause +
                '}';
    }

}
