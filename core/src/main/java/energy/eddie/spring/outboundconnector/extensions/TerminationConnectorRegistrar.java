package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.agnostic.OutboundConnectorExtension;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.core.services.TerminationRouter;

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
