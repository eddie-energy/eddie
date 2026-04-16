// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v1_12;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v1_12.outbound.EnergySharingReferenceDataMarketDocumentOutboundConnector;
import energy.eddie.core.services.v1_12.EnergySharingReferenceDataMarketDocumentService;

import java.util.Optional;

@OutboundConnectorExtension
public class EnergySharingReferenceDataMarketDocumentOutboundRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public EnergySharingReferenceDataMarketDocumentOutboundRegistrar(
            Optional<EnergySharingReferenceDataMarketDocumentOutboundConnector> connector,
            EnergySharingReferenceDataMarketDocumentService cimService
    ) {
        connector.ifPresent(service -> service.setEnergySharingReferenceDataMarketDocumentStream(cimService.getEnergySharingReferenceDataMarketDocumentStream()));
    }
}
