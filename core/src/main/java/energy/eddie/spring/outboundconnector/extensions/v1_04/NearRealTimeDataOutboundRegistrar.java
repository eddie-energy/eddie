// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v1_04;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v1_04.outbound.NearRealTimeDataMarketDocumentOutboundConnector;
import energy.eddie.core.services.v1_04.NearRealTimeDataMarketDocumentService;

import java.util.Optional;

@OutboundConnectorExtension
public class NearRealTimeDataOutboundRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public NearRealTimeDataOutboundRegistrar(
            Optional<NearRealTimeDataMarketDocumentOutboundConnector> rtdConnector,
            NearRealTimeDataMarketDocumentService cimService
    ) {
        rtdConnector.ifPresent(service -> service.setNearRealTimeDataMarketDocumentStream(cimService.getNearRealTimeDataMarketDocumentStream()));
    }
}
