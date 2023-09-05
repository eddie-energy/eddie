package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.FutureStateException;
import energy.eddie.regionconnector.at.api.PastStateException;
import energy.eddie.regionconnector.at.api.PermissionRequest;
import energy.eddie.regionconnector.at.api.PermissionRequestState;
import reactor.core.publisher.Sinks;

public class MessagingPermissionRequest implements PermissionRequest {

    private final PermissionRequest permissionRequest;
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages;

    public MessagingPermissionRequest(PermissionRequest permissionRequest, Sinks.Many<ConnectionStatusMessage> permissionStateMessages) {
        this.permissionRequest = permissionRequest;
        this.permissionStateMessages = permissionStateMessages;
        emitState(PermissionProcessStatus.CREATED);
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
    public String cmRequestId() {
        return permissionRequest.cmRequestId();
    }

    @Override
    public String conversationId() {
        return permissionRequest.conversationId();
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
        emitState(PermissionProcessStatus.VALIDATED);
    }

    @Override
    public void sendToPermissionAdministrator() throws FutureStateException, PastStateException {
        permissionRequest.sendToPermissionAdministrator();
        emitState(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws FutureStateException, PastStateException {
        permissionRequest.receivedPermissionAdministratorResponse();
        emitState(PermissionProcessStatus.RECEIVED_PERMISSION_ADMINISTRATOR_RESPONSE);
    }

    @Override
    public void terminate() throws FutureStateException, PastStateException {
        permissionRequest.terminate();
        emitState(PermissionProcessStatus.TERMINATED);
    }

    @Override
    public void accept() throws FutureStateException, PastStateException {
        permissionRequest.accept();
        emitState(PermissionProcessStatus.ACCEPTED);
    }

    @Override
    public void invalid() throws FutureStateException, PastStateException {
        permissionRequest.invalid();
        emitState(PermissionProcessStatus.INVALID);
    }

    @Override
    public void rejected() throws FutureStateException, PastStateException {
        permissionRequest.rejected();
        emitState(PermissionProcessStatus.REJECTED);
    }

    private void emitState(PermissionProcessStatus processStatus) {
        permissionStateMessages.tryEmitNext(
                new ConnectionStatusMessage(
                        connectionId(),
                        permissionId(),
                        processStatus
                )
        );
    }
}
