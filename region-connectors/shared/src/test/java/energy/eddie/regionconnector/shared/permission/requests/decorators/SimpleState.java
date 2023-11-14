package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.PermissionRequestState;

public class SimpleState implements PermissionRequestState {
    private PermissionProcessStatus status;

    public SimpleState() {
    }

    public SimpleState(PermissionProcessStatus status) {
        this.status = status;
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
    }

    @Override
    public void validate() {

    }

    @Override
    public void sendToPermissionAdministrator() {

    }

    @Override
    public void receivedPermissionAdministratorResponse() {

    }

    @Override
    public void accept() {

    }

    @Override
    public void invalid() {

    }

    @Override
    public void reject() {

    }

    @Override
    public void terminate() {

    }

    @Override
    public void revoke() {

    }

    @Override
    public void timeLimit() {

    }

    @Override
    public void timeOut() {

    }
}
