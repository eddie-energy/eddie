package energy.eddie.spring.outboundconnector.extensions.v0_82;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.core.services.v0_82.TerminationRouter;

import java.util.Optional;

@OutboundConnectorExtension
public class TerminationConnectorRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public TerminationConnectorRegistrar(
            Optional<TerminationConnector> terminationConnector,
            TerminationRouter router
    ) {
        terminationConnector.ifPresent(router::registerTerminationConnector);
    }
}
