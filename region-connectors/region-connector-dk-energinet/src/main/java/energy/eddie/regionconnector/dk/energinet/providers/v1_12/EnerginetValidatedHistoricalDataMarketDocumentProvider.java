// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
import energy.eddie.regionconnector.dk.energinet.providers.HistoricalDataInput;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("energinetValidatedHistoricalDataMarketDocumentProviderV112")
public class EnerginetValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetValidatedHistoricalDataMarketDocumentProvider.class);
    private final Flux<Envelope> eddieValidatedHistoricalDataMarketDocumentFlux;
    private final CommonInformationModelConfiguration cimConfig;

    public EnerginetValidatedHistoricalDataMarketDocumentProvider(
            EnergyDataStreams streams,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        this.cimConfig = cimConfig;
        this.eddieValidatedHistoricalDataMarketDocumentFlux = streams.historicalDataInputs()
                .mapNotNull(this::mapToValidatedHistoricalMarketDocument)
                .share();
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataV112Stream() {
        return eddieValidatedHistoricalDataMarketDocumentFlux;
    }

    @Nullable
    private Envelope mapToValidatedHistoricalMarketDocument(
            HistoricalDataInput response
    ) {
        try {
            return new IntermediateValidatedHistoricalDataMarketDocument(
                    cimConfig, response.source(), response.metadata()
            ).value();
        } catch (Exception e) {
            LOGGER.error("Failed to map to validated historical market document", e);
            return null;
        }
    }
}
