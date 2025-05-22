package energy.eddie.spring.outboundconnector.extensions.v1_04;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v1_04.outbound.ValidatedHistoricalDataMarketDocumentOutboundConnector;
import energy.eddie.core.services.v1_04.ValidatedHistoricalDataMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

@OutboundConnectorExtension
public class ValidatedHistoricalDataOutboundRegistrar {

    public ValidatedHistoricalDataOutboundRegistrar(
            ObjectProvider<ValidatedHistoricalDataMarketDocumentOutboundConnector> vhdConnector,
            ValidatedHistoricalDataMarketDocumentService cimService
    ) {
        vhdConnector.ifAvailable(service -> service.setValidatedHistoricalDataMarketDocumentStream(cimService.getValidatedHistoricalDataMarketDocumentStream()));
    }
}
