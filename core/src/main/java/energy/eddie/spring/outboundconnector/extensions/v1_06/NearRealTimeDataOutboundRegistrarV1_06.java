package energy.eddie.spring.outboundconnector.extensions.v1_06;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v1_06.outbound.NearRealTimeDataMarketDocumentOutboundConnectorV1_06;
import energy.eddie.core.services.v1_06.NearRealTimeDataMarketDocumentService;

import java.util.Optional;

@OutboundConnectorExtension
@SuppressWarnings("java:S101")
public class NearRealTimeDataOutboundRegistrarV1_06 {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public NearRealTimeDataOutboundRegistrarV1_06(
            Optional<NearRealTimeDataMarketDocumentOutboundConnectorV1_06> rtdConnector,
            NearRealTimeDataMarketDocumentService cimService
    ) {
        rtdConnector.ifPresent(service -> service.setNearRealTimeDataMarketDocumentStreamV1_06(cimService.getNearRealTimeDataMarketDocumentStream()));
    }
}
