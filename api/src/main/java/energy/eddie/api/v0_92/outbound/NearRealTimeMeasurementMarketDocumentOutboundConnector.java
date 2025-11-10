package energy.eddie.api.v0_92.outbound;

import energy.eddie.cim.v0_92.nrtmd.ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument;
import reactor.core.publisher.Flux;

public interface NearRealTimeMeasurementMarketDocumentOutboundConnector {

    void setNearRealTimeDataMeasurementMarketDocumentStream(
            Flux<ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument> marketDocumentStream
    );
}
