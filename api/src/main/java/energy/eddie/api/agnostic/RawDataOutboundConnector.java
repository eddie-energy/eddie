package energy.eddie.api.agnostic;

import reactor.core.publisher.Flux;

public interface RawDataOutboundConnector {
    void setRawDataStream(Flux<RawDataMessage> rawDataStream);
}
