// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_04;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("DatadisValidatedHistoricalDataMarketDocumentProviderV1_04")
public class DatadisValidatedHistoricalDataMarketDocumentProvider implements ValidatedHistoricalDataMarketDocumentProvider {
    private final Flux<IdentifiableMeteringData> identifiableMeterReadings;
    private final DatadisConfiguration datadisConfig;
    private final CommonInformationModelConfiguration cimConfig;

    public DatadisValidatedHistoricalDataMarketDocumentProvider(
            EnergyDataStreams streams,
            DatadisConfiguration datadisConfig,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            CommonInformationModelConfiguration cimConfig
    ) {
        this.identifiableMeterReadings = streams.getValidatedHistoricalData();
        this.datadisConfig = datadisConfig;
        this.cimConfig = cimConfig;
    }

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings
                .map(id -> new IntermediateValidatedHistoricalDocument(id, cimConfig, datadisConfig))
                .map(IntermediateValidatedHistoricalDocument::value);
    }
}
