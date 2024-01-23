package energy.eddie.regionconnector.shared.permission.requests.extensions;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.ConnectionStatusMessage;
import reactor.core.publisher.Sinks;

public class MessagingExtension<T extends PermissionRequest> implements Extension<T> {
    private final Sinks.Many<ConnectionStatusMessage> messages;

    public MessagingExtension(Sinks.Many<ConnectionStatusMessage> messages) {
        this.messages = messages;
    }

    @Override
    public void accept(T permissionRequest) {
        messages.tryEmitNext(
                new ConnectionStatusMessage(
                        permissionRequest.connectionId(),
                        permissionRequest.permissionId(),
                        permissionRequest.dataNeedId(),
                        permissionRequest.dataSourceInformation(),
                        permissionRequest.state().status(),
                        ""
                )
        );
    }
}