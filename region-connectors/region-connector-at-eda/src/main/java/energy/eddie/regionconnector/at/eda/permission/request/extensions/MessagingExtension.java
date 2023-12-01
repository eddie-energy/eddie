package energy.eddie.regionconnector.at.eda.permission.request.extensions;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import reactor.core.publisher.Sinks;

public class MessagingExtension implements Extension<AtPermissionRequest> {
    private final Sinks.Many<ConnectionStatusMessage> messages;

    public MessagingExtension(Sinks.Many<ConnectionStatusMessage> messages) {
        this.messages = messages;
    }

    @Override
    public void accept(AtPermissionRequest permissionRequest) {
        messages.tryEmitNext(
                new ConnectionStatusMessage(
                        permissionRequest.connectionId(),
                        permissionRequest.permissionId(),
                        permissionRequest.dataNeedId(),
                        permissionRequest.state().status()
                )
        );
    }
}
