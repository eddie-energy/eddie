// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AccountingPointDataProvider {
    private final Flux<AccountingPointEnvelope> accountingPointEnvelopeFlux;

    public AccountingPointDataProvider(PublishService publishService, Jaxb2Marshaller marshaller) {
        accountingPointEnvelopeFlux = publishService.accountingPointData()
                                                    .map(id -> new IntermediateAccountingPointMarketDocument(id,
                                                                                                             marshaller))
                                                    .flatMapIterable(IntermediateAccountingPointMarketDocument::toAps);
    }

    @MessageStream(AccountingPointEnvelope.class)
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return accountingPointEnvelopeFlux;
    }
}
