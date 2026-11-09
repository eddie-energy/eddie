// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("DatadisValidatedHistoricalDataMarketDocumentProviderV1_04")
public class DatadisValidatedHistoricalDataMarketDocumentProvider {
    private final Flux<VHDEnvelope> validatedHistoricalData;

    public DatadisValidatedHistoricalDataMarketDocumentProvider(
            EnergyDataStreams streams,
            DatadisConfiguration datadisConfig,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            CommonInformationModelConfiguration cimConfig
    ) {
        var identifiableMeterReadings = streams.getValidatedHistoricalData();
        this.validatedHistoricalData = identifiableMeterReadings
                .map(id -> new IntermediateValidatedHistoricalDocument(id, cimConfig, datadisConfig))
                .map(IntermediateValidatedHistoricalDocument::value)
                .share();
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalData;
    }
}
