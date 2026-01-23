// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * This class is for processing incoming master data by mapping it to {@link AccountingPointEnvelope}
 */
@Component
public class EdaAccountingPointEnvelopeProvider implements AccountingPointEnvelopeProvider {

    private final Flux<AccountingPointEnvelope> apFlux;

    public EdaAccountingPointEnvelopeProvider(
            IdentifiableStreams streams,
            IntermediateAccountingPointMarketDocumentFactory factory
    ) {
        this.apFlux = streams.masterDataStream()
                             .map(factory::create)
                             .map(IntermediateAccountingPointMarketDocument::accountingPointEnvelope);
    }

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return apFlux;
    }

    @Override
    public void close() throws Exception {
        // Nothing to clean up, flux is closed when the underlying flux is closed
    }
}
