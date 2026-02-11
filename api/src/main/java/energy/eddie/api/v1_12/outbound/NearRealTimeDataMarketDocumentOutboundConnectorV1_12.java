package energy.eddie.api.v1_12.outbound;

import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import reactor.core.publisher.Flux;

/**
 * An outbound connector delivers data to the eligible party. All messages from the region connectors
 * are passed on to the EP using their transport of choice, e.g. Kafka.
 */
@SuppressWarnings("java:S114")
public interface NearRealTimeDataMarketDocumentOutboundConnectorV1_12 {
    @SuppressWarnings("java:S100")
    void setNearRealTimeDataMarketDocumentStreamV1_12(
            Flux<RTDEnvelope> marketDocumentStream
    );
}
