// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.provider.agnostic;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.agnostic.RawDataMessage;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@OnRawDataMessagesEnabled
public class AiidaRawDataProvider {
    private final Flux<RawDataMessage> flux;

    public AiidaRawDataProvider(IdentifiableStreams streams) {
        this.flux = streams.rawDataMessageFlux();
    }

    @MessageStream(RawDataMessage.class)
    public Flux<RawDataMessage> getRawDataStream() {
        return flux;
    }
}
