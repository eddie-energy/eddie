package energy.eddie.regionconnector.de.eta.providers.v1_04;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * CIM v1.04 Validated Historical Data provider for the German ETA Plus connector.
 */
@Component("deValidatedHistoricalDataMarketDocumentProviderV104")
public class DeValidatedHistoricalDataMarketDocumentProvider {

    private final Flux<IdentifiableValidatedHistoricalData> identifiableData;
    private final CommonInformationModelConfiguration cimConfig;
    private final DeEtaPlusConfiguration deConfiguration;

    public DeValidatedHistoricalDataMarketDocumentProvider(
            ValidatedHistoricalDataStream stream,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration deConfiguration
    ) {
        this.identifiableData = stream.validatedHistoricalData();
        this.cimConfig = cimConfig;
        this.deConfiguration = deConfiguration;
    }

    @MessageStream(VHDEnvelope.class)
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableData
                .map(data -> new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, deConfiguration, data))
                .flatMapIterable(IntermediateValidatedHistoricalDataMarketDocument::toVhd);
    }
}