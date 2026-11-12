// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.streams.HistoricalDataInput;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@SuppressWarnings("java:S6830")
@Component("FluviusValidatedHistoricalDataEnvelopeProviderV1_04")
public class FluviusValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FluviusValidatedHistoricalDataMarketDocumentProvider.class);
    private final Flux<VHDEnvelope> validatedHistoricalData;

    private final FluviusOAuthConfiguration fluviusConfig;

    public FluviusValidatedHistoricalDataMarketDocumentProvider(
            FluviusOAuthConfiguration fluviusConfig,
            IdentifiableDataStreams identifiableDataStreams
    ) {
        this.fluviusConfig = fluviusConfig;
        var identifiableMeterReadings = identifiableDataStreams.historicalDataInputs();
        this.validatedHistoricalData = identifiableMeterReadings
                .concatMap(input -> Mono.fromCallable(() -> getIntermediateVHD(input).toVHD())
                        .onErrorResume(error -> {
                            LOGGER.warn("Failed to map historical data input", error);
                            return Mono.empty();
                        }))
                .share();
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalData;
    }

    private IntermediateValidatedHistoricalDocument getIntermediateVHD(HistoricalDataInput identifiableMeteringData) {
        return new IntermediateValidatedHistoricalDocument(
                fluviusConfig, identifiableMeteringData.source(), identifiableMeteringData.metadata()
        );
    }
}
