// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("CimStreamsV1_04")
public class CimStreams {
    private final EnergyDataService energyDataService;

    public CimStreams(EnergyDataService energyDataService) {
        this.energyDataService = energyDataService;
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return energyDataService.getIdentifiableValidatedHistoricalDataStream()
                                .flatMapIterable(id -> new IntermediateValidatedHistoricalDataMarketDocument(id.payload(),
                                                                                                             id.permissionRequest()).toVhds());
    }
}
