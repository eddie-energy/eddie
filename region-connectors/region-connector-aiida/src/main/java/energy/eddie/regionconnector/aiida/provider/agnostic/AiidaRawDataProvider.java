// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.provider.agnostic;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AiidaRawDataProvider implements RawDataProvider {
    private final Flux<RawDataMessage> flux;

    public AiidaRawDataProvider(IdentifiableStreams streams) {
        this.flux = streams.rawDataMessageFlux();
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return flux;
    }

    @Override
    public void close() {
        // No-Op
    }
}
