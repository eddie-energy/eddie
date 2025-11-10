package energy.eddie.spring.outboundconnector.extensions.v0_92;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v0_92.outbound.NearRealTimeMeasurementMarketDocumentOutboundConnector;
import energy.eddie.core.services.v0_92.NearRealTimeMeasurementMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

@OutboundConnectorExtension
public class NearRealTimeDataMarketDocumentOutboundRegistrar {

    public NearRealTimeDataMarketDocumentOutboundRegistrar(
            ObjectProvider<NearRealTimeMeasurementMarketDocumentOutboundConnector> connector,
            NearRealTimeMeasurementMarketDocumentService cimService
    ) {
        connector.ifAvailable(service -> service.setNearRealTimeDataMeasurementMarketDocumentStream(cimService.getNearRealTimeMeasurementMarketDocumentStream()));
    }
}
