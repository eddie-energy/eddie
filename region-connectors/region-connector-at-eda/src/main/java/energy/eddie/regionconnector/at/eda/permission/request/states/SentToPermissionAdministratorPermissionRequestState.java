package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.regionconnector.at.api.ContextualizedPermissionRequestState;
import energy.eddie.regionconnector.at.api.FutureStateException;
import energy.eddie.regionconnector.at.api.PastStateException;
import energy.eddie.regionconnector.at.api.PermissionRequest;

public class SentToPermissionAdministratorPermissionRequestState extends ContextualizedPermissionRequestState {

    public SentToPermissionAdministratorPermissionRequestState(PermissionRequest permissionRequest) {
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
    public void accept() {
        permissionRequest.changeState(new AcceptedPermissionRequestState(permissionRequest));
    }

    @Override
    public void invalid() {
        permissionRequest.changeState(new InvalidPermissionRequestState(permissionRequest));
    }

    @Override
    public void reject() {
        permissionRequest.changeState(new RejectedPermissionRequestState(permissionRequest));
    }

    @Override
    public void terminate() throws FutureStateException {
        throw new FutureStateException(this);
    }
}
