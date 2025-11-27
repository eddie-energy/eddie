// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.provider.v1_04;

import energy.eddie.api.v1_04.NearRealTimeDataMarketDocumentProviderV1_04;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component(value = "aiidaNearRealTimeDataMarketDocumentProviderV104")
public class AiidaNearRealTimeDataMarketDocumentProvider implements NearRealTimeDataMarketDocumentProviderV1_04 {
    private final Flux<RTDEnvelope> flux;

    public AiidaNearRealTimeDataMarketDocumentProvider(IdentifiableStreams streams) {
        this.flux = streams.nearRealTimeDataCimV104Flux();
    }

    @Override
    public Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentsStream() {
        return flux;
    }
}
