// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.agnostic;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.opaque.RegionConnectorOpaqueEnvelopeService;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.agnostic.OpaqueEnvelopeRouter;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code OpaqueEnvelopeRouterRegistrar} should be added to each region connector's own context and will
 * register the {@link RegionConnector} of each region connector to the common {@link OpaqueEnvelopeRouterRegistrar}.
 * Each region connector implementation is required to provide an implementation of the {@code RegionConnector} interface.
 */
@RegionConnectorExtension
public class OpaqueEnvelopeRouterRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    // In this case the opaque envelope router might be nullable.
    public OpaqueEnvelopeRouterRegistrar(
            RegionConnector regionConnector,
            Optional<RegionConnectorOpaqueEnvelopeService> opaqueEnvelopeService,
            Optional<OpaqueEnvelopeRouter> opaqueEnvelopeRouter
    ) {
        requireNonNull(regionConnector);
        requireNonNull(opaqueEnvelopeService);
        requireNonNull(opaqueEnvelopeRouter);

        if (opaqueEnvelopeService.isEmpty() || opaqueEnvelopeRouter.isEmpty()) {
            return;
        }

        opaqueEnvelopeRouter.get().registerOpaqueEnvelopeService(
                regionConnector.getMetadata().id(),
                opaqueEnvelopeService.get()
        );
    }
}
