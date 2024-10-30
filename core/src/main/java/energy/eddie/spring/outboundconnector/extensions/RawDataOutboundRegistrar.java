package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.agnostic.outbound.RawDataOutboundConnector;
import energy.eddie.core.services.RawDataService;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@OutboundConnectorExtension
@OnRawDataMessagesEnabled
public class RawDataOutboundRegistrar {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public RawDataOutboundRegistrar(
            Optional<RawDataOutboundConnector> rawDataConnector,
            RawDataService rawDataService,
            @Value("${eddie.raw.data.output.enabled:false}") boolean enabled
    ) {
        if (enabled) {
            rawDataConnector.ifPresent(service -> service.setRawDataStream(rawDataService.getRawDataStream()));
        }
    }
}
