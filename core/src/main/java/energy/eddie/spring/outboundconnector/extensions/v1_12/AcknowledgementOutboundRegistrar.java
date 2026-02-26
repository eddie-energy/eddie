// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v1_12;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v1_12.outbound.AcknowledgementMarketDocumentOutboundConnector;
import energy.eddie.core.services.v1_12.AcknowledgementMarketDocumentService;

import java.util.Optional;

@OutboundConnectorExtension
@SuppressWarnings("java:S101")
public class AcknowledgementOutboundRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public AcknowledgementOutboundRegistrar(
            Optional<AcknowledgementMarketDocumentOutboundConnector> ackConnector,
            AcknowledgementMarketDocumentService cimService
    ) {
        ackConnector.ifPresent(service -> service.setAcknowledgementMarketDocumentStream(cimService.getAcknowledgementMarketDocumentStream()));
    }
}
