package energy.eddie.outbound.rest;

import energy.eddie.api.agnostic.outbound.OutboundConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OutboundConnector(name = "rest")
@SpringBootApplication
public class RestOutboundConnector {
}
