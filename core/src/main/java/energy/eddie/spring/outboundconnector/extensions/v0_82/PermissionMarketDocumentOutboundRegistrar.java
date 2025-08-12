package energy.eddie.spring.outboundconnector.extensions.v0_82;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v0_82.outbound.PermissionMarketDocumentOutboundConnector;
import energy.eddie.core.services.v0_82.PermissionMarketDocumentService;

import java.util.Optional;

@OutboundConnectorExtension
public class PermissionMarketDocumentOutboundRegistrar {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public PermissionMarketDocumentOutboundRegistrar(
            Optional<PermissionMarketDocumentOutboundConnector> pmdConnector,
            PermissionMarketDocumentService cimService
    ) {
        pmdConnector.ifPresent(service -> service.setPermissionMarketDocumentStream(cimService.getPermissionMarketDocumentStream()));
    }
}
