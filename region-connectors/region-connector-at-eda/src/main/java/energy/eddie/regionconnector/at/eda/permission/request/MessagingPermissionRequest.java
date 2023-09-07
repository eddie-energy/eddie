package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import reactor.core.publisher.Sinks;

public class MessagingPermissionRequest
        extends energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest<AtPermissionRequest>
        implements AtPermissionRequest {

    public MessagingPermissionRequest(AtPermissionRequest permissionRequest, Sinks.Many<ConnectionStatusMessage> permissionStateMessages) {
        super(permissionRequest, permissionStateMessages);
    }

    @Override
    public String cmRequestId() {
        return permissionRequest.cmRequestId();
    }

    @Override
    public String conversationId() {
        return permissionRequest.conversationId();
    }
}
