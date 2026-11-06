// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.shared.cim.v1_12.ValidatedHistoricalDataV112Converter;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("CimStreamsV1_04")
public class CimStreams {
    private final Flux<VHDEnvelope> validatedHistoricalData;
    private final EnergyDataService energyDataService;

    public CimStreams(EnergyDataService energyDataService) {
        this.energyDataService = energyDataService;
        this.validatedHistoricalData = energyDataService.getIdentifiableValidatedHistoricalDataStream()
                                .flatMapIterable(id -> new IntermediateValidatedHistoricalDataMarketDocument(id.payload(),
                                                                                                             id.permissionRequest()).toVhds()).share();
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
