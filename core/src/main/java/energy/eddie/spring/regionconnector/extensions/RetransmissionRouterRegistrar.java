// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.core.services.CoreRetransmissionRouter;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code RetransmissionRouterRegistrar} should be added to each region connector's own context and will
 * register the {@link RegionConnectorRetransmissionService} of each region connector to the common {@link CoreRetransmissionRouter}.
 */
@RegionConnectorExtension
public class RetransmissionRouterRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public RetransmissionRouterRegistrar(
            @Qualifier(RegionConnectorNameExtension.REGION_CONNECTOR_NAME_BEAN_NAME) String regionConnectorName,
            Optional<RegionConnectorRetransmissionService> regionConnectorRetransmissionService,
            Optional<CoreRetransmissionRouter> retransmissionRouter
    ) {
        requireNonNull(regionConnectorName);
        requireNonNull(regionConnectorRetransmissionService);
        requireNonNull(retransmissionRouter);

        if (regionConnectorRetransmissionService.isEmpty() || retransmissionRouter.isEmpty()) {
            return;
        }

        retransmissionRouter.get().registerRetransmissionService(
                regionConnectorName,
                regionConnectorRetransmissionService.get()
        );
    }
}
