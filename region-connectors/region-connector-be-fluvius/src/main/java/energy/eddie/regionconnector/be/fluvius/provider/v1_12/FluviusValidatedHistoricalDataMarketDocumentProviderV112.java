// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


@SuppressWarnings("java:S6830")
@Component("FluviusValidatedHistoricalDataEnvelopeProviderV1_12")
public class FluviusValidatedHistoricalDataMarketDocumentProviderV112 {

    private final Flux<IdentifiableMeteringData> identifiableMeterReadings;
    private final FluviusOAuthConfiguration fluviusConfig;

    public FluviusValidatedHistoricalDataMarketDocumentProviderV112(
            FluviusOAuthConfiguration fluviusConfig,
            IdentifiableDataStreams identifiableDataStreams
    ) {
        this.fluviusConfig = fluviusConfig;
        this.identifiableMeterReadings = identifiableDataStreams.getMeteringData();
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings.map(this::getIntermediateVHD)
                                        .mapNotNull(IntermediateValidatedHistoricalDocument::toVHD);
    }

    private IntermediateValidatedHistoricalDocument getIntermediateVHD(IdentifiableMeteringData identifiableMeteringData) {
        return new IntermediateValidatedHistoricalDocument(fluviusConfig, identifiableMeteringData);
    }
}
