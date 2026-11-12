// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component("validatedHistoricalDataMarketDocumentProviderV112")
public class ValidatedHistoricalDataMarketDocumentProviderV112 {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataMarketDocumentProviderV112.class);
    private final Flux<Envelope> validatedHistoricalData;

    public ValidatedHistoricalDataMarketDocumentProviderV112(
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

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataV112Stream() {
        return validatedHistoricalData;
    }
}
