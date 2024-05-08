package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.api.v0_82.EddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class NlValidatedHistoricalDataMarketDocumentProvider implements EddieValidatedHistoricalDataMarketDocumentProvider {
    private final Flux<EddieValidatedHistoricalDataMarketDocument> flux;
    private final CommonInformationModelConfiguration cimConfig;
    private final MijnAansluitingConfiguration mijnAansluitingConfig;

    public NlValidatedHistoricalDataMarketDocumentProvider(
            PollingService pollingService,
            CommonInformationModelConfiguration cimConfig,
            MijnAansluitingConfiguration mijnAansluitingConfig
    ) {
        this.flux = pollingService.identifiableMeteredDataFlux()
                                  .flatMap(this::mapToVhd);
        this.cimConfig = cimConfig;
        this.mijnAansluitingConfig = mijnAansluitingConfig;
    }

    private Flux<EddieValidatedHistoricalDataMarketDocument> mapToVhd(IdentifiableMeteredData identifiableMeteredData) {
        IntermediateValidatedHistoricalDataMarketDocument vhd = new IntermediateValidatedHistoricalDataMarketDocument(
                cimConfig,
                mijnAansluitingConfig,
                identifiableMeteredData
        );
        var docs = vhd.toEddieValidatedHistoricalDataMarketDocuments();
        if (docs.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(docs);
    }

    @Override
    public Flux<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return flux;
    }

    @Override
    public void close() {
        // No-Op
    }
}
