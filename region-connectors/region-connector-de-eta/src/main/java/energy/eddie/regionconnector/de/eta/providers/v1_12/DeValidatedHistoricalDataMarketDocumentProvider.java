// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component("deValidatedHistoricalDataMarketDocumentProviderV112")
public class DeValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeValidatedHistoricalDataMarketDocumentProvider.class);
    private final Flux<Envelope> validatedHistoricalData;

    public DeValidatedHistoricalDataMarketDocumentProvider(
            ValidatedHistoricalDataStream stream,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration deConfiguration
    ) {
        var identifiableData = stream.historicalDataInputs();
        this.validatedHistoricalData = identifiableData
                .concatMap(input -> Flux.defer(() -> {
                    var documents = new IntermediateValidatedHistoricalDataMarketDocument(
                            cimConfig,
                            deConfiguration,
                            input.source(),
                            input.metadata()
                    ).toVhd();
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
