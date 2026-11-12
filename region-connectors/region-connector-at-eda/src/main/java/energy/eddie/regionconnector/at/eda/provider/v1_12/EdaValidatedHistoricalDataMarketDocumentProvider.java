// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.at.eda.provider.HistoricalDataInput;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component("edaValidatedHistoricalDataMarketDocumentProviderV112")
public class EdaValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaValidatedHistoricalDataMarketDocumentProvider.class);
    private final Flux<Envelope> flux;
    private final CommonInformationModelConfiguration cimConfig;

    public EdaValidatedHistoricalDataMarketDocumentProvider(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            IdentifiableStreams streams
    ) {
        this.cimConfig = cimConfig;
        this.flux = streams.historicalDataInputs()
                .flatMapIterable(this::getToVhd)
                .share();
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataV112Stream() {
        return flux;
    }

    private Iterable<Envelope> getToVhd(HistoricalDataInput consumptionRecord) {
        try {
            return new IntermediateValidatedHistoricalDataMarketDocument(
                    cimConfig, consumptionRecord.source(), consumptionRecord.metadata()
            ).toVhd();
        } catch (Exception e) {
            LOGGER.warn("Caught exception when mapping consumption record to VHD", e);
        }
        return List.of();
    }
}
