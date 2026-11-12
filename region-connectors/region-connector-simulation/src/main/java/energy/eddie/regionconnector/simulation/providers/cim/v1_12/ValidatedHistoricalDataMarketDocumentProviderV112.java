// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ValidatedHistoricalDataMarketDocumentProviderV112 {
    private final DocumentStreams streams;
    private final CommonInformationModelConfiguration cimConfig;

    public ValidatedHistoricalDataMarketDocumentProviderV112(
            DocumentStreams streams,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        this.streams = streams;
        this.cimConfig = cimConfig;
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return streams.getSimulatedMeterReadingStream()
                      .map(d -> new IntermediateValidatedHistoricalDataMarketDocument(d, cimConfig))
                      .map(IntermediateValidatedHistoricalDataMarketDocument::value);
    }
}
