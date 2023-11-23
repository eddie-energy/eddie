package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.StateTransitionException;
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
    public void validate() throws StateTransitionException {
        permissionRequest.validate();
        emitState();
    }

    @Override
    public void sendToPermissionAdministrator() throws StateTransitionException {
        permissionRequest.sendToPermissionAdministrator();
        emitState();
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws StateTransitionException {
        permissionRequest.receivedPermissionAdministratorResponse();
        emitState();
    }

    @Override
    public void terminate() throws StateTransitionException {
        permissionRequest.terminate();
        emitState();
    }

    @Override
    public void accept() throws StateTransitionException {
        permissionRequest.accept();
        emitState();
    }

    @Override
    public void invalid() throws StateTransitionException {
        permissionRequest.invalid();
        emitState();
    }

    @Override
    public void rejected() throws StateTransitionException {
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
