package energy.eddie.spring.outboundconnector.extensions.v0_91_08;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v0_91_08.outbound.ValidatedHistoricalDataMarketDocumentOutboundConnector;
import energy.eddie.core.services.v0_91_08.ValidatedHistoricalDataMarketDocumentService;
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
