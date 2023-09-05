package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.regionconnector.at.api.ContextualizedPermissionRequestState;
import energy.eddie.regionconnector.at.api.PastStateException;
import energy.eddie.regionconnector.at.api.PermissionRequest;

public class AcceptedPermissionRequestState extends ContextualizedPermissionRequestState {

    public AcceptedPermissionRequestState(PermissionRequest permissionRequest) {
        super(permissionRequest);
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
    public void terminate() {
        throw new IllegalStateException("Not implemented yet");
    }
}
