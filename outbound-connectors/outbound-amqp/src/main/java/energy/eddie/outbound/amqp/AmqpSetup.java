package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import energy.eddie.outbound.shared.TopicConfiguration;

public class AmqpSetup {
    private final Connection connection;
    private final TopicConfiguration configuration;

    public AmqpSetup(Connection connection, TopicConfiguration configuration) {
        this.connection = connection;
        this.configuration = configuration;
    }

    public void buildTopology() {
        var topics = new String[]{
                configuration.rawDataMessage(),
                configuration.connectionStatusMessage(),
                configuration.permissionMarketDocument(),
                configuration.accountingPointMarketDocument(),
                configuration.validatedHistoricalDataMarketDocument(),
                configuration.terminationMarketDocument(),
                configuration.redistributionTransactionRequestDocument()
        };
        try (var management = connection.management()) {
            for (var name : topics) {
                management.exchange(name)
                          .declare();
                management.queue(name)
                          .declare();
                management.binding()
                          .sourceExchange(name)
                          .destinationQueue(name)
                          .bind();
            }
        }
    }
}
