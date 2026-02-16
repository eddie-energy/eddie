// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.v1_12;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v1_12.outbound.RegionConnectorMinMaxEnvelopeService;
import energy.eddie.core.services.v1_12.MinMaxEnvelopeRouter;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorNameExtension;
import org.springframework.beans.factory.annotation.Qualifier;

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
            @Qualifier(RegionConnectorNameExtension.REGION_CONNECTOR_NAME_BEAN_NAME) String regionConnectorName,
            Optional<RegionConnectorMinMaxEnvelopeService> minMaxEnvelopeService,
            Optional<MinMaxEnvelopeRouter> minMaxEnvelopeRouter
    ) {
        requireNonNull(regionConnectorName);
        requireNonNull(minMaxEnvelopeService);
        requireNonNull(minMaxEnvelopeRouter);

        if (minMaxEnvelopeService.isEmpty() || minMaxEnvelopeRouter.isEmpty()) {
            return;
        }

        minMaxEnvelopeRouter.get().registerMinMaxEnvelopeService(
                regionConnectorName,
                minMaxEnvelopeService.get()
        );
    }
}
