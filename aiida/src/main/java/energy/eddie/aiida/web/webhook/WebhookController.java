package energy.eddie.aiida.web.webhook;

import energy.eddie.aiida.web.webhook.dtos.ClientConnackRequest;
import energy.eddie.aiida.web.webhook.dtos.ClientDisconnectedRequest;
import energy.eddie.aiida.web.webhook.dtos.WebhookRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookController.class);

    @PostMapping("/event")
    public ResponseEntity<String> event(@RequestBody WebhookRequest payload) {
        var clientId = payload.clientId().replaceAll("[\n\r]", "_");

        switch (payload) {
            case ClientConnackRequest clientConnackPayload -> LOGGER.debug(
                    "Received event {} with status {} from client {}",
                    payload.action(),
                    clientConnackPayload.connAck(),
                    clientId);
            case ClientDisconnectedRequest clientDisconnectedPayload -> {
                var reason = clientDisconnectedPayload.reason().replaceAll("[\n\r]", "_");
                LOGGER.debug("Received event {} with reason {} from client {}", payload.action(), reason, clientId);
            }

            default -> LOGGER.debug("Event {} not supported.", payload.action());
        }

        return ResponseEntity.ok().build();
    }
}
