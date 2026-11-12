package energy.eddie.regionconnector.de.eta.providers.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * CIM v1.04 Validated Historical Data provider for the German ETA Plus connector.
 */
@Component("deValidatedHistoricalDataMarketDocumentProviderV104")
public class DeValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeValidatedHistoricalDataMarketDocumentProvider.class);
    private final Flux<VHDEnvelope> validatedHistoricalData;

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

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalData;
    }
}
