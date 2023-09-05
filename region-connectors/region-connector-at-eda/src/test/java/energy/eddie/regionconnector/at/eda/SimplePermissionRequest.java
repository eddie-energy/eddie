package energy.eddie.regionconnector.at.eda;

import energy.eddie.regionconnector.at.api.PermissionRequest;
import energy.eddie.regionconnector.at.api.PermissionRequestState;

public record SimplePermissionRequest(String permissionId, String connectionId, String cmRequestId,
                                      String conversationId,
                                      PermissionRequestState state) implements PermissionRequest {

    public SimplePermissionRequest(String permissionId, String connectionId) {
        this(permissionId, connectionId, null, null, null);
    }

    @Override
    public void changeState(PermissionRequestState state) {

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
    public void terminate() {

    }

    @Override
    public void accept() {

    }

    @Override
    public void invalid() {

    }

    @Override
    public void rejected() {

    }
}
