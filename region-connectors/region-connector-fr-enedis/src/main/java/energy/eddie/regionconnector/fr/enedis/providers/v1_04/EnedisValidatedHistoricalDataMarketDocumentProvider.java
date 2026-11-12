// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("EnedisValidatedHistoricalDataMarketDocumentProvider_v1_04")
public class EnedisValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnedisValidatedHistoricalDataMarketDocumentProvider.class);
    private final Flux<VHDEnvelope> validatedHistoricalData;

    public EnedisValidatedHistoricalDataMarketDocumentProvider(
            EnergyDataStreams streams,
            EnedisConfiguration enedisConfig
    ) {
        var identifiableMeterReadings = streams.historicalDataInputs();
        this.validatedHistoricalData = identifiableMeterReadings
                .concatMap(input -> Flux.defer(() -> {
                    var document = new IntermediateValidatedHistoricalDocument(
                            input.source(),
                            enedisConfig,
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
