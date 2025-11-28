package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import energy.eddie.outbound.shared.TopicConfiguration;
import energy.eddie.outbound.shared.TopicStructure;

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
                configuration.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_0_82),
                configuration.nearRealTimeDataMarketDocument(),
                configuration.terminationMarketDocument(),
                configuration.redistributionTransactionRequestDocument(),
                configuration.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_1_04),
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
