package energy.eddie.regionconnector.at.eda.provider.v1_04;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EdaValidatedHistoricalDataMarketDocumentProvider implements ValidatedHistoricalDataMarketDocumentProvider {

    private final Flux<VHDEnvelope> flux;

    public EdaValidatedHistoricalDataMarketDocumentProvider(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            IdentifiableStreams streams
    ) {
        this.flux = streams.consumptionRecordStream()
                           .map(res -> new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, res))
                           .flatMapIterable(IntermediateValidatedHistoricalDataMarketDocument::toVhd);
    }

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return flux;
    }
}
