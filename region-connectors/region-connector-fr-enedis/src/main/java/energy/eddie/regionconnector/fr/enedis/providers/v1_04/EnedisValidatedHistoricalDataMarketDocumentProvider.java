// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.shared.cim.v1_12.ValidatedHistoricalDataV112Converter;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("EnedisValidatedHistoricalDataMarketDocumentProvider_v1_04")
public class EnedisValidatedHistoricalDataMarketDocumentProvider {
    private final Flux<VHDEnvelope> validatedHistoricalData;

    private final Flux<IdentifiableMeterReading> identifiableMeterReadings;
    private final EnedisConfiguration enedisConfig;

    public EnedisValidatedHistoricalDataMarketDocumentProvider(
            EnergyDataStreams streams,
            EnedisConfiguration enedisConfig
    ) {
        this.identifiableMeterReadings = streams.getValidatedHistoricalData();
        this.enedisConfig = enedisConfig;
        this.validatedHistoricalData = identifiableMeterReadings
                .map(id -> new IntermediateValidatedHistoricalDocument(id, enedisConfig))
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
