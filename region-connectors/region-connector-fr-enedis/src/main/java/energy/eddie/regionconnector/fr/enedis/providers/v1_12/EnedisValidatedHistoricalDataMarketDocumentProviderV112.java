// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("EnedisValidatedHistoricalDataMarketDocumentProvider_v1_12")
public class EnedisValidatedHistoricalDataMarketDocumentProviderV112 {

    private final Flux<IdentifiableMeterReading> identifiableMeterReadings;
    private final EnedisConfiguration enedisConfig;

    public EnedisValidatedHistoricalDataMarketDocumentProviderV112(
            EnergyDataStreams streams,
            EnedisConfiguration enedisConfig
    ) {
        this.identifiableMeterReadings = streams.getValidatedHistoricalData();
        this.enedisConfig = enedisConfig;
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings
                .map(id -> new IntermediateValidatedHistoricalDocument(id, enedisConfig))
                .map(IntermediateValidatedHistoricalDocument::value);
    }
}
