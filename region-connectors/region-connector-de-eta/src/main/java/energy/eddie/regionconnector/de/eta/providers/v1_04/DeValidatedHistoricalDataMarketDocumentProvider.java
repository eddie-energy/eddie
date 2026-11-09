package energy.eddie.regionconnector.de.eta.providers.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * CIM v1.04 Validated Historical Data provider for the German ETA Plus connector.
 */
@Component("deValidatedHistoricalDataMarketDocumentProviderV104")
public class DeValidatedHistoricalDataMarketDocumentProvider {
    private final Flux<VHDEnvelope> validatedHistoricalData;

    public DeValidatedHistoricalDataMarketDocumentProvider(
            ValidatedHistoricalDataStream stream,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration deConfiguration
    ) {
        var identifiableData = stream.validatedHistoricalData();
        this.validatedHistoricalData = identifiableData
                .map(data -> new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, deConfiguration, data))
                .flatMapIterable(IntermediateValidatedHistoricalDataMarketDocument::toVhd)
                .share();
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return validatedHistoricalData;
    }
}
