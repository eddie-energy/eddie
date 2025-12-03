package energy.eddie.regionconnector.dk.energinet.providers.v1_04;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("EnerginetValidatedHistoricalDataMarketDocumentProvider_v1_04")
public class EnerginetValidatedHistoricalDataMarketDocumentProvider implements ValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetValidatedHistoricalDataMarketDocumentProvider.class);
    private final Flux<VHDEnvelope> eddieValidatedHistoricalDataMarketDocumentFlux;
    private final CommonInformationModelConfiguration cimConfig;

    public EnerginetValidatedHistoricalDataMarketDocumentProvider(
            EnergyDataStreams streams,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        this.eddieValidatedHistoricalDataMarketDocumentFlux = streams.getValidatedHistoricalDataStream()
                                                                     .mapNotNull(this::mapToValidatedHistoricalMarketDocument)
                                                                     .share();
        this.cimConfig = cimConfig;
    }

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return eddieValidatedHistoricalDataMarketDocumentFlux;
    }

    @Nullable
    private VHDEnvelope mapToValidatedHistoricalMarketDocument(
            IdentifiableApiResponse response
    ) {
        try {
            return new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, response).value();
        } catch (Exception e) {
            LOGGER.error("Failed to map to validated historical market document", e);
            return null;
        }
    }
}
