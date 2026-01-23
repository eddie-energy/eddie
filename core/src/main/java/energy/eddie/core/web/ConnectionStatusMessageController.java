// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.core.services.PermissionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;

@RestController
public class ConnectionStatusMessageController {
    private final PermissionService permissionService;

    ConnectionStatusMessageController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping(value = CONNECTION_STATUS_STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ConnectionStatusMessage>> connectionStatusMessageByPermissionId(
            @PathVariable("permission-id") String permissionId
    ) {
        var messages = permissionService
                .getConnectionStatusMessageStream()
                .filter(message -> message.permissionId().equals(permissionId));

        return ResponseEntity.ok()
                             // Tell reverse proxies like Nginx not to buffer the response
                             .header("X-Accel-Buffering", "no")
                             .header("Connection", "keep-alive")
                             .body(messages);
    }
}
