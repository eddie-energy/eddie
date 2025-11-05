package energy.eddie.spring.outboundconnector.extensions.v1_04;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v1_04.outbound.NearRealTimeDataMarketDocumentOutboundConnector;
import energy.eddie.core.services.v1_04.NearRealTimeDataMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

@OutboundConnectorExtension
public class NearRealTimeDataOutboundRegistrar {
    // TODO: add tests for this class
    public NearRealTimeDataOutboundRegistrar(
            ObjectProvider<NearRealTimeDataMarketDocumentOutboundConnector> rtdConnector,
            NearRealTimeDataMarketDocumentService cimService
    ) {
        rtdConnector.ifAvailable(service -> service.setNearRealTimeDataMarketDocumentStream(cimService.getNearRealTimeDataMarketDocumentStream()));
    }
}
