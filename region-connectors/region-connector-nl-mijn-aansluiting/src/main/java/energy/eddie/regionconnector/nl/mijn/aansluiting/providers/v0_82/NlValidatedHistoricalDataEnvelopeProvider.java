package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class NlValidatedHistoricalDataEnvelopeProvider implements ValidatedHistoricalDataEnvelopeProvider {
    private final Flux<ValidatedHistoricalDataEnvelope> flux;
    private final CommonInformationModelConfiguration cimConfig;
    private final MijnAansluitingConfiguration mijnAansluitingConfig;

    public NlValidatedHistoricalDataEnvelopeProvider(
            PollingService pollingService,
            CommonInformationModelConfiguration cimConfig,
            MijnAansluitingConfiguration mijnAansluitingConfig
    ) {
        this.flux = pollingService.identifiableMeteredDataFlux()
                                  .flatMap(this::mapToVhd);
        this.cimConfig = cimConfig;
        this.mijnAansluitingConfig = mijnAansluitingConfig;
    }

    private Flux<ValidatedHistoricalDataEnvelope> mapToVhd(IdentifiableMeteredData identifiableMeteredData) {
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
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return flux;
    }

    @Override
    public void close() {
        // No-Op
    }
}
