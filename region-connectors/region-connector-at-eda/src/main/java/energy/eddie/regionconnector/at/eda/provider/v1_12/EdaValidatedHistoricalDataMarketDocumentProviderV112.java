// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Provides validated historical data as CIM v1.12 documents.
 * Maps the same consumption records as the providers of the other CIM versions, without depending on them.
 */
@Component
public class EdaValidatedHistoricalDataMarketDocumentProviderV112 {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaValidatedHistoricalDataMarketDocumentProviderV112.class);
    private final Flux<Envelope> flux;
    private final CommonInformationModelConfiguration cimConfig;

    public EdaValidatedHistoricalDataMarketDocumentProviderV112(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            IdentifiableStreams streams
    ) {
        this.cimConfig = cimConfig;
        this.flux = streams.consumptionRecordStream()
                           .flatMapIterable(this::toVhd);
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return flux;
    }

    private Iterable<Envelope> toVhd(IdentifiableConsumptionRecord consumptionRecord) {
        try {
            return new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, consumptionRecord).toVhd();
        } catch (Exception e) {
            LOGGER.warn("Caught exception when mapping consumption record to CIM v1.12 VHD", e);
        }
        return List.of();
    }
}
