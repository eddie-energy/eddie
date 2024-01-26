package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.TerminationRouter;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code TerminationRouterRegistrar} should be added to each region connector's own context and will
 * register the {@link RegionConnector} of each region connector to the common {@link TerminationRouter}.
 * Each region connector implementation is required to provide an implementation of the {@code RegionConnector} interface.
 * If there is no termination router, we cannot register the region connectors and termination will be disabled.
 */
@RegionConnectorExtension
public class TerminationRouterRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // In this case the termination router might be nullable.
    public TerminationRouterRegistrar(RegionConnector regionConnector, Optional<TerminationRouter> terminationRouter) {
        requireNonNull(regionConnector);
        requireNonNull(terminationRouter);
        terminationRouter.ifPresent(tr -> tr.registerRegionConnector(regionConnector));
    }
}
