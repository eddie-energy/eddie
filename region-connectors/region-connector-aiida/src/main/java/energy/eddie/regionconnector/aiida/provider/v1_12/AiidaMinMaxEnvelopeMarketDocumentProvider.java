// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.provider.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AiidaMinMaxEnvelopeMarketDocumentProvider {
    private final Flux<RECMMOEEnvelope> flux;

    public AiidaMinMaxEnvelopeMarketDocumentProvider(IdentifiableStreams streams) {
        this.flux = streams.minMaxEnvelopeCimV112Flux();
    }

    @MessageStream(RECMMOEEnvelope.class)
    public Flux<RECMMOEEnvelope> getMinMaxEnvelopesStream() {
        return flux;
    }
}
