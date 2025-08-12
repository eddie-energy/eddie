package energy.eddie.spring.outboundconnector.extensions.v1_04;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.core.services.CoreRetransmissionRouter;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@OutboundConnectorExtension
public class RetransmissionOutboundConnectorRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public RetransmissionOutboundConnectorRegistrar(
            Optional<RetransmissionOutboundConnector> retransmissionConnector,
            Optional<CoreRetransmissionRouter> retransmissionRouter
    ) {
        requireNonNull(retransmissionConnector);
        requireNonNull(retransmissionRouter);

        if (retransmissionConnector.isEmpty() || retransmissionRouter.isEmpty()) {
            return;
        }

        retransmissionRouter.get().registerRetransmissionConnector(retransmissionConnector.get());
        retransmissionConnector.get().setRetransmissionResultStream(retransmissionRouter.get().retransmissionResults());
    }
}
