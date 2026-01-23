// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v0_82;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector;
import energy.eddie.core.services.v0_82.ValidatedHistoricalDataEnvelopeService;

import java.util.Optional;

@OutboundConnectorExtension
public class ValidatedHistoricalDataOutboundRegistrar {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ValidatedHistoricalDataOutboundRegistrar(
            Optional<ValidatedHistoricalDataEnvelopeOutboundConnector> vhdConnector,
            ValidatedHistoricalDataEnvelopeService cimService
    ) {
        vhdConnector.ifPresent(service -> service.setEddieValidatedHistoricalDataMarketDocumentStream(cimService.getEddieValidatedHistoricalDataMarketDocumentStream()));
    }
}
