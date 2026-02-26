// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.provider.v1_12;

import energy.eddie.api.v1_12.AcknowledgementMarketDocumentProvider;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component(value = "aiidaAcknowledgementMarketDocumentProviderV112")
public class AiidaAcknowledgementMarketDocumentProvider implements AcknowledgementMarketDocumentProvider {
    private final Flux<AcknowledgementEnvelope> flux;

    public AiidaAcknowledgementMarketDocumentProvider(IdentifiableStreams streams) {
        this.flux = streams.acknowledgementCimFlux();
    }

    @Override
    public Flux<AcknowledgementEnvelope> getAcknowledgementDataMarketDocumentsStream() {
        return flux;
    }
}
