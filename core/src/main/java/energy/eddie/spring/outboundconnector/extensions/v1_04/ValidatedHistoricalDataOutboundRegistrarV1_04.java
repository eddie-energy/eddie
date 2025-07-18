package energy.eddie.spring.outboundconnector.extensions.v1_04;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v1_04.outbound.ValidatedHistoricalDataMarketDocumentOutboundConnector;
import energy.eddie.core.services.v1_04.ValidatedHistoricalDataMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

// Warning because of the V1_04 postfix, but is required to distinguish the different versions of the same registrar.
@SuppressWarnings("java:S101")
@OutboundConnectorExtension
public class ValidatedHistoricalDataOutboundRegistrarV1_04 {

    public ValidatedHistoricalDataOutboundRegistrarV1_04(
            ObjectProvider<ValidatedHistoricalDataMarketDocumentOutboundConnector> vhdConnector,
            ValidatedHistoricalDataMarketDocumentService cimService
    ) {
        vhdConnector.ifAvailable(service -> service.setValidatedHistoricalDataMarketDocumentStream(cimService.getValidatedHistoricalDataMarketDocumentStream()));
    }
}
