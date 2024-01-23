package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;

public class TransitionableState implements PermissionRequestState {
    private final PermissionProcessStatus initialStatus;
    private final PermissionProcessStatus targetStatus;
    private PermissionProcessStatus currentStatus;

    public TransitionableState(PermissionProcessStatus initialStatus, PermissionProcessStatus targetStatus) {
        this.initialStatus = initialStatus;
        this.targetStatus = targetStatus;
        this.currentStatus = initialStatus;
    }

    private void transition() {
        currentStatus = targetStatus;
    }

    @Override
    public PermissionProcessStatus status() {
        return currentStatus;
    }

    @Override
    public void validate() {
        transition();
    }

    @Override
    public void sendToPermissionAdministrator() {
        transition();
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        transition();
    }

    @Override
    public void accept() {
        transition();
    }

    @Override
    public void invalid() {
        transition();
    }

    @Override
    public void reject() {
        transition();
    }

    @Override
    public void terminate() {
        transition();
    }

    @Override
    public void revoke() {

    }

    @Override
    public void fulfill() {

    }

    @Override
    public void timeOut() {

    }

    @Override
    public String toString() {
        return "TransitionableState{" +
                "initialStatus=" + initialStatus +
                ", targetStatus=" + targetStatus +
                ", currentStatus=" + currentStatus +
                '}';
    }
}