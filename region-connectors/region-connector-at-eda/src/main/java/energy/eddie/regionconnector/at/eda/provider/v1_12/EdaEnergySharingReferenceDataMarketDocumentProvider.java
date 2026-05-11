// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EdaEnergySharingReferenceDataMarketDocumentProvider {
    private final Flux<ESRDMDEnvelope> esrdmdFlux;

    public EdaEnergySharingReferenceDataMarketDocumentProvider(IdentifiableStreams streams) {
        esrdmdFlux = streams.ecmpListStream()
                            .map(IntermediateEnergySharingReferenceDataMarketDocument::new)
                            .map(IntermediateEnergySharingReferenceDataMarketDocument::toEnvelope);
    }

    @MessageStream(ESRDMDEnvelope.class)
    public Flux<ESRDMDEnvelope> getEnergySharingReferenceDataMarketDocumentStream() {
        return esrdmdFlux;
    }
}
