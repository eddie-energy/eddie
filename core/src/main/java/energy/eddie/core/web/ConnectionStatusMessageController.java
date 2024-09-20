package energy.eddie.core.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.core.services.PermissionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/connection-status-messages")
public class ConnectionStatusMessageController {
    private final PermissionService permissionService;

    ConnectionStatusMessageController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping(value = "{permission-id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ConnectionStatusMessage> connectionStatusMessageByPermissionId(
            @PathVariable("permission-id") String permissionId
    ) {
        return permissionService
                .getConnectionStatusMessageStream()
                .filter(message -> message.permissionId().equals(permissionId));
    }
}
