// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.cds.providers.cim.CimStruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class CdsValidatedHistoricalDataMarketDocumentProvider {
    private final Flux<ValidatedHistoricalDataEnvelope> validatedHistoricalDataEnvelopeFlux;

    public CdsValidatedHistoricalDataMarketDocumentProvider(IdentifiableDataStreams streams) {
        validatedHistoricalDataEnvelopeFlux = streams.validatedHistoricalData()
                                                     .map(res -> new IntermediateValidatedHistoricalDataMarketDocument(
                                                             res.permissionRequest(),
                                                             new CimStruct(res).get()
                                                     ))
                                                     .flatMapIterable(IntermediateValidatedHistoricalDataMarketDocument::toVhds);
    }

    @MessageStream(ValidatedHistoricalDataEnvelope.class)
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalDataEnvelopeFlux;
    }
}
