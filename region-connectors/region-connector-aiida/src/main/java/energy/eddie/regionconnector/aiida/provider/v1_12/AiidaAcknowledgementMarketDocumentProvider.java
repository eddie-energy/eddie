// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.provider.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component(value = "aiidaAcknowledgementMarketDocumentProviderV112")
public class AiidaAcknowledgementMarketDocumentProvider {
    private final Flux<AcknowledgementEnvelope> flux;

    public AiidaAcknowledgementMarketDocumentProvider(IdentifiableStreams streams) {
        this.flux = streams.acknowledgementCimFlux();
    }

    @MessageStream(AcknowledgementEnvelope.class)
    public Flux<AcknowledgementEnvelope> getAcknowledgementDataMarketDocumentsStream() {
        return flux;
    }
}
