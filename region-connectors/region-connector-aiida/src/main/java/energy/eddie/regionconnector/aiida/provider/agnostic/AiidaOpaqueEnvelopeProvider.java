// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.provider.agnostic;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AiidaOpaqueEnvelopeProvider {
    private final Flux<OpaqueEnvelope> flux;

    public AiidaOpaqueEnvelopeProvider(IdentifiableStreams streams) {
        this.flux = streams.opaqueEnvelopeFlux();
    }

    @MessageStream(OpaqueEnvelope.class)
    public Flux<OpaqueEnvelope> getOpaqueEnvelopesStream() {
        return flux;
    }
}
