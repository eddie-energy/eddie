// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka.v1_12;

import energy.eddie.api.v1_12.outbound.MinMaxEnvelopeOutboundConnector;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.outbound.shared.TopicStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class MinMaxEnvelopeKafkaConnector implements MinMaxEnvelopeOutboundConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinMaxEnvelopeKafkaConnector.class);
    private final Sinks.Many<RECMMOEEnvelope> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public Flux<RECMMOEEnvelope> getMinMaxEnvelopes() {
        return sink.asFlux();
    }

    @KafkaListener(
            groupId = "min-max-envelope-group",
            id = "eddie-min-max-envelope-listener",
            topics = "fw.${outbound-connector.kafka.eddie-id}." + TopicStructure.CIM_1_12_VALUE + "." + TopicStructure.MIN_MAX_ENVELOPE_MD_VALUE,
            containerFactory = "minMaxEnvelopeListenerContainerFactory"
    )
    public void process(
            @Payload RECMMOEEnvelope payload
    ) {
        LOGGER.atDebug()
              .addArgument(() -> payload.getMarketDocument().getMRID())
              .log("Got new min-max envelope {}");
        sink.tryEmitNext(payload);
    }
}
