package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_92;

import energy.eddie.api.v0_92.NearRealTimeMeasurementMarketDocumentProvider;
import energy.eddie.cim.v0_92.nrtmd.ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class NlNearRealTimeMeasurementMarketDocumentProvider implements NearRealTimeMeasurementMarketDocumentProvider {
    private final Flux<ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument> flux;

    public NlNearRealTimeMeasurementMarketDocumentProvider(
            PollingService pollingService,
            MijnAansluitingConfiguration config
    ) {
        flux = pollingService.identifiableMeteredDataFlux()
                             .map(identifiableMeteredData -> new IntermediateRealTimeMeasurementMarketDocument(
                                     identifiableMeteredData,
                                     config
                             ))
                             .flatMapIterable(IntermediateRealTimeMeasurementMarketDocument::value);
    }

    @Override
    public Flux<ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument> getNearRealTimeMeasurementMarketDocumentsStream() {
        return flux;
    }
}
