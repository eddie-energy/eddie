package energy.eddie.spring.outboundconnector.extensions.v1_12;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v1_12.outbound.NearRealTimeDataMarketDocumentOutboundConnectorV1_12;
import energy.eddie.core.services.v1_12.NearRealTimeDataMarketDocumentService;

import java.util.Optional;

@OutboundConnectorExtension
@SuppressWarnings("java:S101")
public class NearRealTimeDataOutboundRegistrarV1_12 {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public NearRealTimeDataOutboundRegistrarV1_12(
            Optional<NearRealTimeDataMarketDocumentOutboundConnectorV1_12> rtdConnector,
            NearRealTimeDataMarketDocumentService cimService
    ) {
        rtdConnector.ifPresent(service -> service.setNearRealTimeDataMarketDocumentStreamV1_12(cimService.getNearRealTimeDataMarketDocumentStream()));
    }
}
