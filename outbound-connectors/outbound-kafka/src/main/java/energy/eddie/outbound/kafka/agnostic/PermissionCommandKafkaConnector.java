// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka.agnostic;

import energy.eddie.api.agnostic.outbound.PermissionCommandOutboundConnector;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.outbound.shared.TopicStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class PermissionCommandKafkaConnector implements PermissionCommandOutboundConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCommandKafkaConnector.class);
    private final Sinks.Many<PermissionCommand> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public Flux<PermissionCommand> getPermissionCommands() {
        return sink.asFlux();
    }

    @KafkaListener(
            groupId = "permission-command-group",
            id = "eddie-permission-command-listener",
            topics = "fw.${outbound-connector.kafka.eddie-id}." + TopicStructure.AGNOSTIC_VALUE + "." + TopicStructure.PERMISSION_COMMAND_VALUE,
            containerFactory = "permissionCommandListenerContainerFactory"
    )
    public void process(
            @Payload PermissionCommand payload
    ) {
        LOGGER.debug("Got new permission command");
        sink.tryEmitNext(payload);
    }
}
