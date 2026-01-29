// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_04;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class EdaValidatedHistoricalDataMarketDocumentProvider implements ValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaValidatedHistoricalDataMarketDocumentProvider.class);
    private final Flux<VHDEnvelope> flux;
    private final CommonInformationModelConfiguration cimConfig;

    public EdaValidatedHistoricalDataMarketDocumentProvider(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            IdentifiableStreams streams
    ) {
        this.cimConfig = cimConfig;
        this.flux = streams.consumptionRecordStream()
                           .flatMapIterable(this::getToVhd);
    }

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return flux;
    }

    private Iterable<VHDEnvelope> getToVhd(IdentifiableConsumptionRecord consumptionRecord) {
        try {
            return new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, consumptionRecord).toVhd();
        } catch (Exception e) {
            LOGGER.warn("Caught exception when mapping consumption record to VHD", e);
        }
        return List.of();
    }
}
