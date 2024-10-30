package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v0_82.outbound.AccountingPointEnvelopeOutboundConnector;
import energy.eddie.core.services.AccountingPointEnvelopeService;

import java.util.Optional;

@OutboundConnectorExtension
public class AccountingPointDataOutboundRegistrar {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public AccountingPointDataOutboundRegistrar(
            Optional<AccountingPointEnvelopeOutboundConnector> apConnector,
            AccountingPointEnvelopeService cimService
    ) {
        apConnector.ifPresent(service -> service.setAccountingPointEnvelopeStream(cimService.getAccountingPointEnvelopeStream()));
    }
}
