// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("EnerginetValidatedHistoricalDataMarketDocumentProvider_v1_12")
public class EnerginetValidatedHistoricalDataMarketDocumentProviderV112 {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetValidatedHistoricalDataMarketDocumentProviderV112.class);
    private final Flux<Envelope> eddieValidatedHistoricalDataMarketDocumentFlux;
    private final CommonInformationModelConfiguration cimConfig;

    public EnerginetValidatedHistoricalDataMarketDocumentProviderV112(
            EnergyDataStreams streams,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        this.eddieValidatedHistoricalDataMarketDocumentFlux = streams.getValidatedHistoricalDataStream()
                                                                     .mapNotNull(this::mapToValidatedHistoricalMarketDocument)
                                                                     .share();
        this.cimConfig = cimConfig;
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return eddieValidatedHistoricalDataMarketDocumentFlux;
    }

    @Nullable
    private Envelope mapToValidatedHistoricalMarketDocument(
            IdentifiableApiResponse response
    ) {
        try {
            return new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, response).value();
        } catch (Exception e) {
            LOGGER.error("Failed to map to CIM v1.12 validated historical market document", e);
            return null;
        }
    }
}
