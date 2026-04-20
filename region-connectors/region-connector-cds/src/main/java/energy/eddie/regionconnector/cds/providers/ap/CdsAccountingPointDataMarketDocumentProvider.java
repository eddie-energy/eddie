// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.ap;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class CdsAccountingPointDataMarketDocumentProvider {
    private final IdentifiableDataStreams streams;

    public CdsAccountingPointDataMarketDocumentProvider(IdentifiableDataStreams streams) {this.streams = streams;}

    @MessageStream(AccountingPointEnvelope.class)
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return streams.accountingPointData()
                      .map(IntermediateAccountingPointDocument::new)
                      .flatMapIterable(IntermediateAccountingPointDocument::toAp);
    }
}
