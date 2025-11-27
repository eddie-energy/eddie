// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v1_04;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v1_04.outbound.NearRealTimeDataMarketDocumentOutboundConnectorV1_04;
import energy.eddie.core.services.v1_04.NearRealTimeDataMarketDocumentService;

import java.util.Optional;

@OutboundConnectorExtension
@SuppressWarnings("java:S101")
public class NearRealTimeDataOutboundRegistrarV1_04 {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public NearRealTimeDataOutboundRegistrarV1_04(
            Optional<NearRealTimeDataMarketDocumentOutboundConnectorV1_04> rtdConnector,
            NearRealTimeDataMarketDocumentService cimService
    ) {
        rtdConnector.ifPresent(service -> service.setNearRealTimeDataMarketDocumentStreamV1_04(cimService.getNearRealTimeDataMarketDocumentStream()));
    }
}
