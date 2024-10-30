package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v0_82.outbound.PermissionMarketDocumentOutboundConnector;
import energy.eddie.core.services.PermissionMarketDocumentService;

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
