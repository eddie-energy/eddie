// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v0_82;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.v0_82.outbound.AccountingPointEnvelopeOutboundConnector;
import energy.eddie.core.services.v0_82.AccountingPointEnvelopeService;

import java.util.Optional;

@OutboundConnectorExtension
public class AccountingPointDataOutboundRegistrar {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public AccountingPointDataOutboundRegistrar(
            Optional<AccountingPointEnvelopeOutboundConnector> apConnector,
            AccountingPointEnvelopeService cimService
    ) {
        apConnector.ifPresent(service -> service.setAccountingPointEnvelopeStream(cimService.getAccountingPointEnvelopeStream()));
    }
}
