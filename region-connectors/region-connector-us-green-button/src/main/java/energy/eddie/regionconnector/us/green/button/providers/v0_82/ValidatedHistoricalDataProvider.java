// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.UnsupportedUnitException;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class ValidatedHistoricalDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataProvider.class);
    private final Flux<ValidatedHistoricalDataEnvelope> validatedHistoricalDataEnvelopes;


    public ValidatedHistoricalDataProvider(
            PublishService publishService,
            Jaxb2Marshaller jaxb2Marshaller,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            GreenButtonConfiguration greenButtonConfiguration
    ) {
        validatedHistoricalDataEnvelopes = publishService
                .validatedHistoricalData()
                .map(id -> new IntermediateValidatedHistoricalDataMarketDocument(
                        id,
                        jaxb2Marshaller,
                        cimConfig,
                        greenButtonConfiguration
                ))
                .flatMapIterable(vhd -> {
                    try {
                        return vhd.toVhd();
                    } catch (UnsupportedUnitException e) {
                        LOGGER.warn("Got exception when mapping to validated historical data market document", e);
                        return List.of();
                    }
                });
    }

    @MessageStream(ValidatedHistoricalDataEnvelope.class)
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalDataEnvelopes;
    }
}
