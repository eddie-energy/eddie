package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import energy.eddie.api.agnostic.outbound.OutboundConnector;
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
}
