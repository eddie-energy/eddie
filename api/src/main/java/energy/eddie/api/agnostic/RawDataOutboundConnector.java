package energy.eddie.api.agnostic;

import java.util.concurrent.Flow;

public interface RawDataOutboundConnector {
    void setRawDataStream(Flow.Publisher<RawDataMessage> rawDataStream);
}
