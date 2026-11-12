// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("DatadisValidatedHistoricalDataMarketDocumentProviderV1_12")
public class DatadisValidatedHistoricalDataMarketDocumentProviderV112 {
    private final Flux<IdentifiableMeteringData> identifiableMeterReadings;
    private final DatadisConfiguration datadisConfig;
    private final CommonInformationModelConfiguration cimConfig;

    public DatadisValidatedHistoricalDataMarketDocumentProviderV112(
            EnergyDataStreams streams,
            DatadisConfiguration datadisConfig,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            CommonInformationModelConfiguration cimConfig
    ) {
        this.identifiableMeterReadings = streams.getValidatedHistoricalData();
        this.datadisConfig = datadisConfig;
        this.cimConfig = cimConfig;
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings
                .map(id -> new IntermediateValidatedHistoricalDocument(id, cimConfig, datadisConfig))
                .map(IntermediateValidatedHistoricalDocument::value);
    }
}
