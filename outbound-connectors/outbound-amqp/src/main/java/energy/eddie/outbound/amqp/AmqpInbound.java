package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;
import com.rabbitmq.client.amqp.Message;
import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.outbound.shared.TopicConfiguration;
import energy.eddie.outbound.shared.serde.DeserializationException;
import energy.eddie.outbound.shared.serde.MessageSerde;
import energy.eddie.outbound.shared.serde.RetransmissionRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AmqpInbound implements TerminationConnector, RetransmissionOutboundConnector, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpInbound.class);
    private final Consumer terminationsConsumer;
    private final Consumer retransmissionRequestsConsumer;
    private final Sinks.Many<Pair<String, PermissionEnvelope>> terminations = Sinks.many()
                                                                                   .multicast()
                                                                                   .onBackpressureBuffer();
    private final Sinks.Many<RetransmissionRequest> retransmissionRequests = Sinks.many()
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
        this.serde = serde;
    }

    @Override
    public void close() {
        terminationsConsumer.close();
        retransmissionRequestsConsumer.close();
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
}
