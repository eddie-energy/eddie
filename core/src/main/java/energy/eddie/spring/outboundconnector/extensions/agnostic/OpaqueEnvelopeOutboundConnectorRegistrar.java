// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.agnostic;

import energy.eddie.api.agnostic.outbound.OpaqueEnvelopeOutboundConnector;
import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.core.services.agnostic.OpaqueEnvelopeRouter;

import java.util.Optional;

@OutboundConnectorExtension
public class OpaqueEnvelopeOutboundConnectorRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public OpaqueEnvelopeOutboundConnectorRegistrar(
            Optional<OpaqueEnvelopeOutboundConnector> opaqueEnvelopeOutboundConnector,
            OpaqueEnvelopeRouter router
    ) {
        opaqueEnvelopeOutboundConnector.ifPresent(router::registerOpaqueEnvelopeConnector);
    }
}
