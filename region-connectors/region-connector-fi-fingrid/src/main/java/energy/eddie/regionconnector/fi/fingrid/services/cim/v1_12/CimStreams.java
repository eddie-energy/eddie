// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("CimStreamsV1_12")
public class CimStreams {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimStreams.class);
    private final Flux<Envelope> validatedHistoricalData;

    public CimStreams(EnergyDataService energyDataService) {
        this.validatedHistoricalData = energyDataService.historicalDataInputs()
                .concatMap(input -> Flux.defer(() -> {
                    var documents = new IntermediateValidatedHistoricalDataMarketDocument(
                            input.source().payload(),
                            input.source().permissionRequest(),
                            input.metadata()
                    ).toVhds();
                    return Flux.fromIterable(documents);
                })
                        .onErrorResume(error -> {
                            LOGGER.warn("Failed to map historical data input", error);
                            return Flux.empty();
                        }))
                .share();
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataV112Stream() {
        return validatedHistoricalData;
    }
}
