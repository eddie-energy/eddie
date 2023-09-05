package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public class SentToPermissionAdministratorPermissionRequestState extends ContextualizedPermissionRequestState<AtPermissionRequest> {

    public SentToPermissionAdministratorPermissionRequestState(AtPermissionRequest permissionRequest) {
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
