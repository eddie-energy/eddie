package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import energy.eddie.outbound.shared.Endpoints;

import java.util.List;

public class AmqpSetup {
    private static final List<String> EXCHANGE_AND_QUEUE_NAMES = List.of(
            Endpoints.Agnostic.CONNECTION_STATUS_MESSAGE,
            Endpoints.Agnostic.RAW_DATA_IN_PROPRIETARY_FORMAT,
            Endpoints.V0_82.PERMISSION_MARKET_DOCUMENTS,
            Endpoints.V0_82.VALIDATED_HISTORICAL_DATA,
            Endpoints.V0_82.ACCOUNTING_POINT_MARKET_DOCUMENTS,
            Endpoints.V0_82.TERMINATIONS,
            Endpoints.V0_91_08.RETRANSMISSIONS
    );
    private final Connection connection;

    public AmqpSetup(Connection connection) {
        this.connection = connection;
    }

    public void buildTopology() {
        try (var management = connection.management()) {
            for (var name : EXCHANGE_AND_QUEUE_NAMES) {
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
