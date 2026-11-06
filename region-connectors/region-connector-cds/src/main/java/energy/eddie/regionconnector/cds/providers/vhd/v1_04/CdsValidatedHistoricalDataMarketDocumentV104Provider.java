// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.cds.providers.cim.CimStruct;
import energy.eddie.regionconnector.shared.cim.v1_12.ValidatedHistoricalDataV112Converter;
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
                .flatMapIterable(IntermediateValidatedHistoricalDataMarketDocument::toVhds)
                .share();
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalDataEnvelopeFlux;
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataV112Stream() {
        return getValidatedHistoricalDataMarketDocumentsStream().map(ValidatedHistoricalDataV112Converter::convert);
    }
}
