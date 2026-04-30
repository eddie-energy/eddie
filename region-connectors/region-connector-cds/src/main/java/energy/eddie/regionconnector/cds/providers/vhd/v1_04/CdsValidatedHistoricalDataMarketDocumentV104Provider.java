// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.cds.providers.cim.CimStruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class CdsValidatedHistoricalDataMarketDocumentV104Provider {
    private final Flux<VHDEnvelope> validatedHistoricalDataEnvelopeFlux;

    public CdsValidatedHistoricalDataMarketDocumentV104Provider(IdentifiableDataStreams streams) {
        validatedHistoricalDataEnvelopeFlux = streams.validatedHistoricalData()
                                                     .map(res -> new IntermediateValidatedHistoricalDataMarketDocument(
                                                             res.permissionRequest(),
                                                             new CimStruct(res).get()
                                                     ))
                                                     .flatMapIterable(IntermediateValidatedHistoricalDataMarketDocument::toVhds);
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalDataEnvelopeFlux;
    }
}
