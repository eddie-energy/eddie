package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ValidatedHistoricalDataProvider implements ValidatedHistoricalDataEnvelopeProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataProvider.class);
    private final Flux<ValidatedHistoricalDataEnvelope> validatedHistoricalDataEnvelopes;


    public ValidatedHistoricalDataProvider(
            PublishService publishService,
            Jaxb2Marshaller jaxb2Marshaller,
            CommonInformationModelConfiguration cimConfig,
            GreenButtonConfiguration greenButtonConfiguration
    ) {
        validatedHistoricalDataEnvelopes = publishService
                .flux()
                .map(id -> new IntermediateValidatedHistoricalDataMarketDocument(
                        id,
                        jaxb2Marshaller,
                        cimConfig,
                        greenButtonConfiguration
                ))
                .mapNotNull(vhd -> {
                    try {
                        return vhd.toVhd();
                    } catch (UnsupportedUnitException e) {
                        LOGGER.warn("Got exception when mapping to validated historical data market document", e);
                        return null;
                    }
                })
                .flatMap(Flux::fromIterable);
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalDataEnvelopes;
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
