package energy.eddie.api.agnostic.outbound;

import energy.eddie.api.agnostic.RawDataMessage;
import reactor.core.publisher.Flux;

public interface RawDataOutboundConnector {
    void setRawDataStream(Flux<RawDataMessage> rawDataStream);
}
