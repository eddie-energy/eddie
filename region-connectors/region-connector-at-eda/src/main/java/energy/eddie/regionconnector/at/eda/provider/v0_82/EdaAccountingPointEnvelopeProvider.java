// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * This class is for processing incoming master data by mapping it to {@link AccountingPointEnvelope}
 */
@Component
public class EdaAccountingPointEnvelopeProvider {

    private final Flux<AccountingPointEnvelope> apFlux;

    public EdaAccountingPointEnvelopeProvider(
            IdentifiableStreams streams,
            IntermediateAccountingPointMarketDocumentFactory factory
    ) {
        this.apFlux = streams.masterDataStream()
                             .map(factory::create)
                             .map(IntermediateAccountingPointMarketDocument::accountingPointEnvelope);
    }

    @MessageStream(AccountingPointEnvelope.class)
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return apFlux;
    }
}
