package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import energy.eddie.api.agnostic.outbound.OutboundConnector;
import energy.eddie.outbound.shared.serde.MessageSerde;
import energy.eddie.outbound.shared.serde.SerdeFactory;
import energy.eddie.outbound.shared.serde.SerdeInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@OutboundConnector(name = "amqp")
@SpringBootApplication
public class AmqpOutboundConnector {

    @Bean
    public Environment amqpEnvironment() {
        return new AmqpEnvironmentBuilder()
                .build();
    }

    @Bean
    public Connection connection(Environment environment, @Value("${outbound-connector.amqp.uri}") String uri) {
        var connection = environment.connectionBuilder()
                                    .uri(uri)
                                    .build();
        var setup = new AmqpSetup(connection);
        setup.buildTopology();
        return connection;
    }

    @Bean
    public MessageSerde serde(@Value("${outbound-connector.amqp.format:json}") String format) throws SerdeInitializationException {
        return SerdeFactory.getInstance().create(format);
    }
}
