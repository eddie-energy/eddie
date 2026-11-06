// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.shared.cim.v1_12.ValidatedHistoricalDataV112Converter;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


@SuppressWarnings("java:S6830")
@Component("FluviusValidatedHistoricalDataEnvelopeProviderV1_04")
public class FluviusValidatedHistoricalDataMarketDocumentProvider {
    private final Flux<VHDEnvelope> validatedHistoricalData;

    private final Flux<IdentifiableMeteringData> identifiableMeterReadings;
    private final FluviusOAuthConfiguration fluviusConfig;

    public FluviusValidatedHistoricalDataMarketDocumentProvider(
            FluviusOAuthConfiguration fluviusConfig,
            IdentifiableDataStreams identifiableDataStreams
    ) {
        this.fluviusConfig = fluviusConfig;
        this.identifiableMeterReadings = identifiableDataStreams.getMeteringData();
        this.validatedHistoricalData = identifiableMeterReadings.map(this::getIntermediateVHD)
                                        .mapNotNull(IntermediateValidatedHistoricalDocument::toVHD).share();
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalData;
    }

    private IntermediateValidatedHistoricalDocument getIntermediateVHD(IdentifiableMeteringData identifiableMeteringData) {
        return new IntermediateValidatedHistoricalDocument(fluviusConfig, identifiableMeteringData);
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataV112Stream() {
        return getValidatedHistoricalDataMarketDocumentsStream().map(ValidatedHistoricalDataV112Converter::convert);
    }
}
