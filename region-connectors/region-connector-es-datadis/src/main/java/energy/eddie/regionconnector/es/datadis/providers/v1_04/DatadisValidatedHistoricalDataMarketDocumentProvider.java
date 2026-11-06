// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.shared.cim.v1_12.ValidatedHistoricalDataV112Converter;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("DatadisValidatedHistoricalDataMarketDocumentProviderV1_04")
public class DatadisValidatedHistoricalDataMarketDocumentProvider {
    private final Flux<VHDEnvelope> validatedHistoricalData;
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
        this.validatedHistoricalData = identifiableMeterReadings
                .map(id -> new IntermediateValidatedHistoricalDocument(id, cimConfig, datadisConfig))
                .map(IntermediateValidatedHistoricalDocument::value).share();
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalData;
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataV112Stream() {
        return getValidatedHistoricalDataMarketDocumentsStream().map(ValidatedHistoricalDataV112Converter::convert);
    }
}
