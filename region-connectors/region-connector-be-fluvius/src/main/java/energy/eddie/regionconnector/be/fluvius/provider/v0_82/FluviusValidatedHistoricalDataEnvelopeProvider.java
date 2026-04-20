// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


@Component
public class FluviusValidatedHistoricalDataEnvelopeProvider {

    private final Flux<IdentifiableMeteringData> identifiableMeterReadings;
    private final FluviusOAuthConfiguration fluviusConfig;
    private final DataNeedsService dataNeedsService;

    public FluviusValidatedHistoricalDataEnvelopeProvider(
            FluviusOAuthConfiguration fluviusConfig,
            IdentifiableDataStreams identifiableDataStreams,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService
            ) {
        this.fluviusConfig = fluviusConfig;
        this.identifiableMeterReadings = identifiableDataStreams.getMeteringData();
        this.dataNeedsService = dataNeedsService;
    }

    private IntermediateValidatedHistoricalDocument getIntermediateVHD(IdentifiableMeteringData identifiableMeteringData) {
        return new IntermediateValidatedHistoricalDocument(fluviusConfig, identifiableMeteringData, dataNeedsService);
    }

    @MessageStream(ValidatedHistoricalDataEnvelope.class)
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableMeterReadings.map(this::getIntermediateVHD)
                .flatMapIterable(IntermediateValidatedHistoricalDocument::toVHD);
    }
}
