package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.v0.process.model.*;

import java.time.ZonedDateTime;

public class TimeFramedPermissionRequestAdapter implements TimeframedPermissionRequest {
    private final TimeframedPermissionRequest permissionRequest;
    private final PermissionRequest adaptee;

    public TimeFramedPermissionRequestAdapter(TimeframedPermissionRequest permissionRequest, PermissionRequest adaptee) {
        this.permissionRequest = permissionRequest;
        this.adaptee = adaptee;
    }

    @Override
    public String permissionId() {
        return permissionRequest.permissionId();
    }

    @Override
    public String connectionId() {
        return permissionRequest.connectionId();
    }

    @Override
    public String dataNeedId() {
        return permissionRequest.dataNeedId();
    }

    @Override
    public PermissionRequestState state() {
        return permissionRequest.state();
    }

    @Override
    public void changeState(PermissionRequestState state) {
        permissionRequest.changeState(state);
    }

    @Override
    public ZonedDateTime start() {
        return permissionRequest.start();
    }

    @Override
    public ZonedDateTime end() {
        return permissionRequest.end();
    }

    @Override
    public void validate() throws FutureStateException, PastStateException {
        adaptee.validate();
    }

    @Override
    public void sendToPermissionAdministrator() throws FutureStateException, PastStateException {
        adaptee.sendToPermissionAdministrator();
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws FutureStateException, PastStateException {
        adaptee.receivedPermissionAdministratorResponse();
    }

    @Override
    public void terminate() throws FutureStateException, PastStateException {
        adaptee.terminate();
    }

    @Override
    public void accept() throws FutureStateException, PastStateException {
        adaptee.accept();
    }

    @Override
    public void invalid() throws FutureStateException, PastStateException {
        adaptee.invalid();
    }

    @Override
    public void rejected() throws FutureStateException, PastStateException {
        adaptee.rejected();
    }
}