package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import reactor.core.publisher.Sinks;

public class MessagingPermissionRequest implements PermissionRequest {

    protected final PermissionRequest permissionRequest;
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages;

    public MessagingPermissionRequest(PermissionRequest permissionRequest, Sinks.Many<ConnectionStatusMessage> permissionStateMessages) {
        this.permissionRequest = permissionRequest;
        this.permissionStateMessages = permissionStateMessages;
        emitState();
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
    public void validate() throws FutureStateException, PastStateException {
        permissionRequest.validate();
        emitState();
    }

    @Override
    public void sendToPermissionAdministrator() throws FutureStateException, PastStateException {
        permissionRequest.sendToPermissionAdministrator();
        emitState();
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws FutureStateException, PastStateException {
        permissionRequest.receivedPermissionAdministratorResponse();
        emitState();
    }

    @Override
    public void terminate() throws FutureStateException, PastStateException {
        permissionRequest.terminate();
        emitState();
    }

    @Override
    public void accept() throws FutureStateException, PastStateException {
        permissionRequest.accept();
        emitState();
    }

    @Override
    public void invalid() throws FutureStateException, PastStateException {
        permissionRequest.invalid();
        emitState();
    }

    @Override
    public void rejected() throws FutureStateException, PastStateException {
        permissionRequest.rejected();
        emitState();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PermissionRequest) {
            return permissionRequest.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return permissionRequest.hashCode();
    }

    private void emitState() {
        permissionStateMessages.tryEmitNext(
                new ConnectionStatusMessage(
                        connectionId(),
                        permissionId(),
                        dataNeedId(),
                        permissionRequest.state().status()
                )
        );
    }
}
