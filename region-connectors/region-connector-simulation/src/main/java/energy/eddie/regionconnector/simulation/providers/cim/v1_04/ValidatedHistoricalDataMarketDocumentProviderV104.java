// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ValidatedHistoricalDataMarketDocumentProviderV104 {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataMarketDocumentProviderV104.class);
    private final Flux<VHDEnvelope> validatedHistoricalData;

    public ValidatedHistoricalDataMarketDocumentProviderV104(
            DocumentStreams streams,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        this.validatedHistoricalData = streams.historicalDataInputs()
                .concatMap(input -> Flux.defer(() -> {
                    var document = new IntermediateValidatedHistoricalDataMarketDocument(
                            input.source(),
                            cimConfig,
                            input.metadata()
                    ).value();
                    return Flux.just(document);
                })
                        .onErrorResume(error -> {
                            LOGGER.warn("Failed to map historical data input", error);
                            return Flux.empty();
                        }))
                .share();
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalData;
    }
}
