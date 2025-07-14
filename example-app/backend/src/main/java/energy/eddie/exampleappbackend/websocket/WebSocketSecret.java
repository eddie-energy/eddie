package energy.eddie.exampleappbackend.websocket;

import lombok.Builder;

import java.time.Instant;

@Builder
public record WebSocketSecret(
        String secret,
        Instant expiresAt,
        String allowedTopic
) {
}
