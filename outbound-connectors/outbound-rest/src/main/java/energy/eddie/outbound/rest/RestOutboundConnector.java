package energy.eddie.outbound.rest;

import energy.eddie.api.agnostic.outbound.EnableSwaggerDoc;
import energy.eddie.api.agnostic.outbound.OutboundConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OutboundConnector(name = "rest")
@EnableSwaggerDoc
@SpringBootApplication
public class RestOutboundConnector {
}
