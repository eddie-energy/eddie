// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v1_04;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.UnsupportedUnitException;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component("v104ValidatedHistoricalDataProvider")
public class ValidatedHistoricalDataProvider implements ValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(energy.eddie.regionconnector.us.green.button.providers.v1_04.ValidatedHistoricalDataProvider.class);
    private final Flux<VHDEnvelope> validatedHistoricalDataEnvelopes;

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

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalDataEnvelopes;
    }
}
