// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_04;

import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.cds.providers.cim.CimStruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class CdsValidatedHistoricalDataMarketDocumentV104Provider implements ValidatedHistoricalDataMarketDocumentProvider {
    private final Flux<VHDEnvelope> validatedHistoricalDataEnvelopeFlux;

    public CdsValidatedHistoricalDataMarketDocumentV104Provider(IdentifiableDataStreams streams) {
        validatedHistoricalDataEnvelopeFlux = streams.validatedHistoricalData()
                                                     .map(res -> new IntermediateValidatedHistoricalDataMarketDocument(
                                                             res.permissionRequest(),
                                                             new CimStruct(res).get()
                                                     ))
                                                     .flatMapIterable(IntermediateValidatedHistoricalDataMarketDocument::toVhds);
    }

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalDataEnvelopeFlux;
    }
}
