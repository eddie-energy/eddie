// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.provider.v1_12;

import energy.eddie.api.v1_12.NearRealTimeDataMarketDocumentProviderV1_12;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component(value = "aiidaNearRealTimeDataMarketDocumentProviderV112")
public class AiidaNearRealTimeDataMarketDocumentProvider implements NearRealTimeDataMarketDocumentProviderV1_12 {
    private final Flux<RTDEnvelope> flux;

    public AiidaNearRealTimeDataMarketDocumentProvider(IdentifiableStreams streams) {
        this.flux = streams.nearRealTimeDataCimV112Flux();
    }

    @Override
    public Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentsStream() {
        return flux;
    }
}
