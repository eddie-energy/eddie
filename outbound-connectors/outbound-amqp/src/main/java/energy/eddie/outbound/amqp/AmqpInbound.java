package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;
import com.rabbitmq.client.amqp.Message;
import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.outbound.shared.Endpoints;
import energy.eddie.outbound.shared.serde.DeserializationException;
import energy.eddie.outbound.shared.serde.MessageSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AmqpInbound implements TerminationConnector, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpInbound.class);
    private final Consumer consumer;
    private final Sinks.Many<Pair<String, PermissionEnvelope>> terminations = Sinks.many()
                                                                                   .multicast()
                                                                                   .onBackpressureBuffer();
    private final MessageSerde serde;

    public AmqpInbound(Connection connection, MessageSerde serde) {
        consumer = connection.consumerBuilder()
                             .queue(Endpoints.V0_82.TERMINATIONS)
                             .messageHandler(this::consume)
                             .build();
        this.serde = serde;
    }

    @Override
    public void close() {
        consumer.close();
        terminations.tryEmitComplete();
    }

    @Override
    public Flux<Pair<String, PermissionEnvelope>> getTerminationMessages() {
        return terminations.asFlux();
    }

    private void consume(Consumer.Context context, Message message) {
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
}
