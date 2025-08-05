package energy.eddie.outbound.metric;

import energy.eddie.api.agnostic.outbound.OutboundConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OutboundConnector(name = "metric")
@SpringBootApplication
public class MetricOutboundConnector {
}
