// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka.agnostic;

import energy.eddie.api.agnostic.opaque.OpaqueEnvelope;
import energy.eddie.api.agnostic.outbound.OpaqueEnvelopeOutboundConnector;
import energy.eddie.outbound.shared.TopicStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class OpaqueEnvelopeKafkaConnector implements OpaqueEnvelopeOutboundConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpaqueEnvelopeKafkaConnector.class);
    private final Sinks.Many<OpaqueEnvelope> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public Flux<OpaqueEnvelope> getOpaqueEnvelopes() {
        return sink.asFlux();
    }

    @KafkaListener(
            groupId = "opaque-envelope-group",
            id = "eddie-opaque-envelope-listener",
            topics = "fw.${outbound-connector.kafka.eddie-id}." + TopicStructure.AGNOSTIC_VALUE + "." + TopicStructure.OPAQUE_ENVELOPE_VALUE,
            containerFactory = "opaqueEnvelopeListenerContainerFactory"
    )
    public void process(
            @Payload OpaqueEnvelope payload
    ) {
        LOGGER.debug("Got new opaque envelope");
        sink.tryEmitNext(payload);
    }
}
