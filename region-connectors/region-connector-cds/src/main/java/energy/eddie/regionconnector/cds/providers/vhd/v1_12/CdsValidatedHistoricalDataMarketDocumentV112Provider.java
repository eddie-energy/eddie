// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.cds.providers.cim.CimStruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component("cdsValidatedHistoricalDataMarketDocumentV112Provider")
public class CdsValidatedHistoricalDataMarketDocumentV112Provider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdsValidatedHistoricalDataMarketDocumentV112Provider.class);
    private final Flux<Envelope> validatedHistoricalDataEnvelopeFlux;

    public CdsValidatedHistoricalDataMarketDocumentV112Provider(IdentifiableDataStreams streams) {
        validatedHistoricalDataEnvelopeFlux = streams.historicalDataInputs()
                .concatMap(input -> Flux.defer(() -> {
                    var documents = new IntermediateValidatedHistoricalDataMarketDocument(
                            input.source().permissionRequest(),
                            new CimStruct(input.source()).get(),
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
        return validatedHistoricalDataEnvelopeFlux;
    }
}
