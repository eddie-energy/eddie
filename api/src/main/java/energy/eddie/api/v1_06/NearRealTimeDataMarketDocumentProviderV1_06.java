package energy.eddie.api.v1_06;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.cim.v1_06.rtd.RTDEnvelope;
import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of {@link RTDEnvelope}s available.
 */
@SuppressWarnings("java:S114")
public interface NearRealTimeDataMarketDocumentProviderV1_06 {
    /**
     * Data stream of all RTDEnvelope created by this region connector.
     *
     * @return RTDEnvelope stream
     */
    Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentsStream();
}
