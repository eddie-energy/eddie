package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector;
import energy.eddie.core.services.ValidatedHistoricalDataEnvelopeService;

import java.util.Optional;

@OutboundConnectorExtension
public class ValidatedHistoricalDataOutboundRegistrar {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ValidatedHistoricalDataOutboundRegistrar(
            Optional<ValidatedHistoricalDataEnvelopeOutboundConnector> vhdConnector,
            ValidatedHistoricalDataEnvelopeService cimService
    ) {
        vhdConnector.ifPresent(service -> service.setEddieValidatedHistoricalDataMarketDocumentStream(cimService.getEddieValidatedHistoricalDataMarketDocumentStream()));
    }
}
