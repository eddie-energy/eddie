package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

/**
 * The UnableToSendPermissionRequestState indicates that we were not able to send the permission request to the 3rd party service.
 * This state is recoverable, by retrying to send the permission request.
 */
public class UnableToSendPermissionRequestState extends ContextualizedPermissionRequestState<AtPermissionRequest> {
    private final Throwable cause;

    protected UnableToSendPermissionRequestState(AtPermissionRequest permissionRequest, Throwable cause) {
        super(permissionRequest);
        this.cause = cause;
    }

    @Override
    public String toString() {
        return "UnableToSendPermissionRequestState{" +
                "cause=" + cause +
                '}';
    }

    @Override
    public void validate() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    public void sendToPermissionAdministrator() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    public void accept() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    public void invalid() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    public void reject() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    public void terminate() throws PastStateException {
        throw new PastStateException(this);
    }
}
