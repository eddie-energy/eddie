// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v1_12;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v1_12.outbound.MinMaxEnvelopeOutboundConnector;
import energy.eddie.core.services.v1_12.MinMaxEnvelopeRouter;

import java.util.Optional;

@OutboundConnectorExtension
public class MinMaxEnvelopeOutboundConnectorRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public MinMaxEnvelopeOutboundConnectorRegistrar(
            Optional<MinMaxEnvelopeOutboundConnector> minMaxEnvelopeOutboundConnector,
            MinMaxEnvelopeRouter router
    ) {
        minMaxEnvelopeOutboundConnector.ifPresent(router::registerMinMaxEnvelopeConnector);
    }
}
