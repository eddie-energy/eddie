// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("DatadisValidatedHistoricalDataMarketDocumentProviderV1_04")
public class DatadisValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatadisValidatedHistoricalDataMarketDocumentProvider.class);
    private final Flux<VHDEnvelope> validatedHistoricalData;

    public DatadisValidatedHistoricalDataMarketDocumentProvider(
            EnergyDataStreams streams,
            DatadisConfiguration datadisConfig,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            CommonInformationModelConfiguration cimConfig
    ) {
        var identifiableMeterReadings = streams.historicalDataInputs();
        this.validatedHistoricalData = identifiableMeterReadings
                .concatMap(input -> Flux.defer(() -> {
                    var document = new IntermediateValidatedHistoricalDocument(
                            input.source(),
                            cimConfig,
                            datadisConfig,
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
