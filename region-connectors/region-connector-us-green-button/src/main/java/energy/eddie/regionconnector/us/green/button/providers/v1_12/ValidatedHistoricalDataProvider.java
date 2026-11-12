// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.UnsupportedUnitException;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component("greenButtonValidatedHistoricalDataProviderV112")
public class ValidatedHistoricalDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataProvider.class);
    private final Flux<Envelope> validatedHistoricalDataEnvelopes;

    public ValidatedHistoricalDataProvider(
            PublishService publishService,
            Jaxb2Marshaller jaxb2Marshaller,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            GreenButtonConfiguration greenButtonConfiguration
    ) {
        validatedHistoricalDataEnvelopes = publishService
                .historicalDataInputs()
                .concatMap(input -> Flux.defer(() -> {
                    try {
                        return Flux.fromIterable(new IntermediateValidatedHistoricalDataMarketDocument(
                                input.source(), jaxb2Marshaller, cimConfig, greenButtonConfiguration, input.metadata()
                        ).toVhd());
                    } catch (UnsupportedUnitException error) {
                        LOGGER.warn("Got exception when mapping to validated historical data market document", error);
                        return Flux.empty();
                    }
                })
                        .onErrorResume(error -> {
                            LOGGER.warn("Failed to map historical data input", error);
                            return Flux.empty();
                        }))
                .share();
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataV112Stream() {
        return validatedHistoricalDataEnvelopes;
    }
}
