package energy.eddie.core.services.v0_92;

import energy.eddie.api.v0_92.NearRealTimeMeasurementMarketDocumentProvider;
import energy.eddie.cim.v0_92.nrtmd.ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class NearRealTimeMeasurementMarketDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NearRealTimeMeasurementMarketDocumentService.class);

    private final Sinks.Many<ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument> measurementSink = Sinks.many()
                                                                                                               .multicast()
                                                                                                               .onBackpressureBuffer();


    public void registerProvider(NearRealTimeMeasurementMarketDocumentProvider provider) {
        LOGGER.info("Registering {}", provider.getClass().getName());
        provider.getNearRealTimeMeasurementMarketDocumentsStream()
                .subscribe(measurementSink::tryEmitNext);
    }

    public Flux<ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument> getNearRealTimeMeasurementMarketDocumentStream() {
        return measurementSink.asFlux();
    }
}
