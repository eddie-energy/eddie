// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("DatadisValidatedHistoricalDataEnvelopeProviderV0_82")
public class DatadisValidatedHistoricalDataEnvelopeProvider {
    private final Flux<IdentifiableMeteringData> identifiableMeterReadings;
    private final IntermediateVHDFactory intermediateVHDFactory;

    public DatadisValidatedHistoricalDataEnvelopeProvider(
            EnergyDataStreams streams,
            IntermediateVHDFactory intermediateVHDFactory
    ) {
        this.identifiableMeterReadings = streams.getValidatedHistoricalData();
        this.intermediateVHDFactory = intermediateVHDFactory;
    }

    @MessageStream(ValidatedHistoricalDataEnvelope.class)
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings
                .map(intermediateVHDFactory::create)
                .map(IntermediateValidatedHistoricalDocument::eddieValidatedHistoricalDataMarketDocument);
    }
}
