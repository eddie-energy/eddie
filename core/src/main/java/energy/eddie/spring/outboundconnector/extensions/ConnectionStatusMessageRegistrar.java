package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.agnostic.ConnectionStatusMessageOutboundConnector;
import energy.eddie.api.agnostic.OutboundConnectorExtension;
import energy.eddie.core.services.PermissionService;

import java.util.Optional;

@OutboundConnectorExtension
public class ConnectionStatusMessageRegistrar {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ConnectionStatusMessageRegistrar(
            Optional<ConnectionStatusMessageOutboundConnector> csmConnector,
            PermissionService permissionService
    ) {
        csmConnector.ifPresent(csm -> csm.setConnectionStatusMessageStream(permissionService.getConnectionStatusMessageStream()));
    }
}
