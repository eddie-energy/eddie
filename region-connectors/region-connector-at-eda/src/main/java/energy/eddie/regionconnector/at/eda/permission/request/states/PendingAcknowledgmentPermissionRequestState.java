package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.regionconnector.at.api.ContextualizedPermissionRequestState;
import energy.eddie.regionconnector.at.api.FutureStateException;
import energy.eddie.regionconnector.at.api.PastStateException;
import energy.eddie.regionconnector.at.api.PermissionRequest;

public class PendingAcknowledgmentPermissionRequestState extends ContextualizedPermissionRequestState {

    public PendingAcknowledgmentPermissionRequestState(PermissionRequest permissionRequest) {
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
    public void receivedPermissionAdministratorResponse() {
        permissionRequest.changeState(
                new SentToPermissionAdministratorPermissionRequestState(
                        permissionRequest
                )
        );
    }

    @Override
    public void accept() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    public void invalid() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    public void reject() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    public void terminate() throws FutureStateException {
        throw new FutureStateException(this);
    }
}
