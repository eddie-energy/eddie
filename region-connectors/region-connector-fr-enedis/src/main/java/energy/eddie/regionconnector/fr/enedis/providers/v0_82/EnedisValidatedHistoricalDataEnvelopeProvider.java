// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("EnedisValidatedHistoricalDataEnvelopeProvider_v0_82")
public class EnedisValidatedHistoricalDataEnvelopeProvider {

    private final Flux<IdentifiableMeterReading> identifiableMeterReadings;
    private final IntermediateMarketDocumentFactory intermediateMarketDocumentFactory;

    public EnedisValidatedHistoricalDataEnvelopeProvider(
            EnergyDataStreams streams,
            IntermediateMarketDocumentFactory intermediateMarketDocumentFactory
    ) {
        this.identifiableMeterReadings = streams.getValidatedHistoricalData();
        this.intermediateMarketDocumentFactory = intermediateMarketDocumentFactory;
    }

    @MessageStream(ValidatedHistoricalDataEnvelope.class)
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings
                .map(intermediateMarketDocumentFactory::create)
                .map(IntermediateValidatedHistoricalDocument::eddieValidatedHistoricalDataMarketDocument);
    }
}
