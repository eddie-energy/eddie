// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.shared.cim.v1_12.ValidatedHistoricalDataV112Converter;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ValidatedHistoricalDataMarketDocumentProviderV104 {
    private final Flux<VHDEnvelope> validatedHistoricalData;
    private final DocumentStreams streams;
    private final CommonInformationModelConfiguration cimConfig;

    public ValidatedHistoricalDataMarketDocumentProviderV104(
            DocumentStreams streams,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        this.streams = streams;
        this.cimConfig = cimConfig;
        this.validatedHistoricalData = streams.getSimulatedMeterReadingStream()
                      .map(d -> new IntermediateValidatedHistoricalDataMarketDocument(d, cimConfig))
                      .map(IntermediateValidatedHistoricalDataMarketDocument::value).share();
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalData;
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataV112Stream() {
        return getValidatedHistoricalDataMarketDocumentsStream().map(ValidatedHistoricalDataV112Converter::convert);
    }
}
