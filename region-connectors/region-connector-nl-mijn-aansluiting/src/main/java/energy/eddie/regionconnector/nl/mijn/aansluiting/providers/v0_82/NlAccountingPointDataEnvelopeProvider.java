// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class NlAccountingPointDataEnvelopeProvider {
    private final Flux<AccountingPointEnvelope> flux;

    public NlAccountingPointDataEnvelopeProvider(
            PollingService pollingService,
            MijnAansluitingConfiguration config
    ) {
        flux = pollingService.identifiableAccountingPointDataFlux()
                             .map(res -> new IntermediateAccountingPointDataMarketDocument(res, config))
                             .flatMapIterable(IntermediateAccountingPointDataMarketDocument::toAp);
    }

    @MessageStream(AccountingPointEnvelope.class)
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return flux;
    }
}
