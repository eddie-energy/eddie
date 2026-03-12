// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.v1_12;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v1_12.RegionConnectorMinMaxEnvelopeService;
import energy.eddie.core.services.v1_12.MinMaxEnvelopeRouter;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code MinMaxEnvelopeRouterRegistrar} should be added to each region connector's own context and will
 * register the {@link RegionConnector} of each region connector to the common {@link MinMaxEnvelopeRouter}.
 * Each region connector implemmentation is required to provide an implementation of the {@code RegionConnector} interface.
 */
@RegionConnectorExtension
public class MinMaxEnvelopeRouterRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    // In this case the min-max envelope router might be nullable.
    public MinMaxEnvelopeRouterRegistrar(
            RegionConnector regionConnector,
            Optional<RegionConnectorMinMaxEnvelopeService> minMaxEnvelopeService,
            Optional<MinMaxEnvelopeRouter> minMaxEnvelopeRouter
    ) {
        requireNonNull(regionConnector);
        requireNonNull(minMaxEnvelopeService);
        requireNonNull(minMaxEnvelopeRouter);

        if (minMaxEnvelopeService.isEmpty() || minMaxEnvelopeRouter.isEmpty()) {
            return;
        }

        minMaxEnvelopeRouter.get().registerMinMaxEnvelopeService(
                regionConnector.getMetadata().id(),
                minMaxEnvelopeService.get()
        );
    }
}
