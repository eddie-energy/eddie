package energy.eddie.outbound.amqp;

import energy.eddie.api.agnostic.outbound.OutboundConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OutboundConnector(name = "amqp")
@SpringBootApplication
public class AmqpOutboundConnector {

}
