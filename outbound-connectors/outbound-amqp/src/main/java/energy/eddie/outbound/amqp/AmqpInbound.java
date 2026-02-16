// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;
import com.rabbitmq.client.amqp.Message;
import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.api.v1_12.outbound.MinMaxEnvelopeOutboundConnector;
import energy.eddie.cim.serde.DeserializationException;
import energy.eddie.cim.serde.MessageSerde;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.outbound.shared.TopicConfiguration;
import energy.eddie.outbound.shared.utils.RetransmissionRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AmqpInbound implements TerminationConnector, RetransmissionOutboundConnector, MinMaxEnvelopeOutboundConnector, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpInbound.class);
    private final Consumer terminationsConsumer;
    private final Consumer retransmissionRequestsConsumer;
    private final Consumer minMaxEnvelopeConsumer;
    private final Sinks.Many<Pair<String, PermissionEnvelope>> terminations = Sinks.many()
                                                                                   .multicast()
                                                                                   .onBackpressureBuffer();
    private final Sinks.Many<RetransmissionRequest> retransmissionRequests = Sinks.many()
                                                                                  .multicast()
                                                                                  .onBackpressureBuffer();
    private final Sinks.Many<RECMMOEEnvelope> minMaxEnvelopes = Sinks.many()
                                                                     .multicast()
                                                                     .onBackpressureBuffer();
    private final MessageSerde serde;

    public AmqpInbound(Connection connection, MessageSerde serde, TopicConfiguration config) {
        terminationsConsumer = connection.consumerBuilder()
                                         .queue(config.terminationMarketDocument())
                                         .messageHandler(this::consumeTerminationDocuments)
                                         .build();
        retransmissionRequestsConsumer = connection.consumerBuilder()
                                                   .queue(config.redistributionTransactionRequestDocument())
                                                   .messageHandler(this::consumeRetransmissionRequests)
                                                   .build();
        minMaxEnvelopeConsumer = connection.consumerBuilder()
                                           .queue(config.minMaxEnvelopeDocument())
                                           .messageHandler(this::consumeMinMaxEnvelope)
                                           .build();

        this.serde = serde;
    }

    @Override
    public void close() {
        terminationsConsumer.close();
        retransmissionRequestsConsumer.close();
        minMaxEnvelopeConsumer.close();
        terminations.tryEmitComplete();
        retransmissionRequests.tryEmitComplete();
    }

    @Override
    public Flux<Pair<String, PermissionEnvelope>> getTerminationMessages() {
        return terminations.asFlux();
    }

    @Override
    public Flux<RetransmissionRequest> retransmissionRequests() {
        return retransmissionRequests.asFlux();
    }

    @Override
    public void setRetransmissionResultStream(Flux<RetransmissionResult> retransmissionResultStream) {
        // No-Op
    }

    @Override
    public Flux<RECMMOEEnvelope> getMinMaxEnvelopes() {
        return minMaxEnvelopes.asFlux();
    }

    private void consumeTerminationDocuments(Consumer.Context context, Message message) {
        try {
            var envelope = serde.deserialize(message.body(), PermissionEnvelope.class);
            LOGGER.atDebug()
                  .addArgument(() -> envelope.getPermissionMarketDocument().getMRID())
                  .log("Got new PermissionMarketDocument {}");
            terminations.tryEmitNext(new Pair<>(null, envelope));
            context.accept();
        } catch (DeserializationException e) {
            context.discard();
            LOGGER.info("Got invalid message", e);
        }
    }

    private void consumeRetransmissionRequests(Consumer.Context context, Message message) {
        try {
            var envelope = serde.deserialize(message.body(), RTREnvelope.class);
            LOGGER.atDebug()
                  .addArgument(envelope::getMarketDocumentMRID)
                  .log("Got new retransmission request {}");
            var request = new RetransmissionRequestMapper(envelope);
            retransmissionRequests.tryEmitNext(request.toRetransmissionRequest());
            context.accept();
        } catch (DeserializationException e) {
            context.discard();
            LOGGER.info("Got invalid message", e);
        }
    }

    private void consumeMinMaxEnvelope(Consumer.Context context, Message message) {
        try {
            var envelope = serde.deserialize(message.body(), RECMMOEEnvelope.class);
            LOGGER.atDebug()
                  .addArgument(() -> envelope.getMarketDocument().getMRID())
                  .log("Got new min-max envelope {}");
            minMaxEnvelopes.tryEmitNext(envelope);
            context.accept();
        } catch (DeserializationException e) {
            context.discard();
            LOGGER.info("Got invalid message", e);
        }
    }
}
