package energy.eddie.api.v0_92;

import energy.eddie.cim.v0_92.nrtmd.ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument;
import reactor.core.publisher.Flux;

public interface NearRealTimeMeasurementMarketDocumentProvider {
    /**
     * Data stream of all {@link ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument} created by the region connectors.
     *
     * @return {@link ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument} stream
     */
    Flux<ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument> getNearRealTimeMeasurementMarketDocumentsStream();
}
