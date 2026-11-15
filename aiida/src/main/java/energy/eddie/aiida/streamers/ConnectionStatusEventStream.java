// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.permission.ConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;

/**
 * Publishes changes of the {@link ConnectionStatus} of permissions as soon as an {@link AiidaStreamer} connects to or
 * disconnects from the EDDIE framework.
 */
@Component
public class ConnectionStatusEventStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusEventStream.class);
    private final Sinks.Many<ConnectionStatusEvent> sink = Sinks.many().multicast().directBestEffort();

    /**
     * Publishes the new connection status of a permission. Events are dropped if nobody is subscribed.
     * Synchronized, as streamers publish from their own MQTT client threads.
     */
    public synchronized void publish(UUID userId, UUID permissionId, ConnectionStatus connectionStatus) {
        var result = sink.tryEmitNext(new ConnectionStatusEvent(userId, permissionId, connectionStatus));
        if (result.isFailure() && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            LOGGER.warn("Failed to publish connection status {} for permission {}: {}",
                        connectionStatus,
                        permissionId,
                        result);
        }
    }

    /**
     * Returns the connection status changes of all permissions of the passed user.
     */
    public Flux<ConnectionStatusEvent> events(UUID userId) {
        return sink.asFlux().filter(event -> event.userId().equals(userId));
    }

    public record ConnectionStatusEvent(UUID userId, UUID permissionId, ConnectionStatus connectionStatus) {}
}
