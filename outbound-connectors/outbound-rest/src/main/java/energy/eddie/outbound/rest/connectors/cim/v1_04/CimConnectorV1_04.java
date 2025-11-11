package energy.eddie.outbound.rest.connectors.cim.v1_04;

import energy.eddie.api.v1_04.outbound.NearRealTimeDataMarketDocumentOutboundConnector;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
@SuppressWarnings("java:S101") // Names shouldn't contain underscores, but this is required to not have bean name clashes with the other CimConnector
public class CimConnectorV1_04 implements NearRealTimeDataMarketDocumentOutboundConnector, AutoCloseable {
    private final Sinks.Many<RTDEnvelope> rtdSink = CimConnector.createSink();

    @Override
    public void setNearRealTimeDataMarketDocumentStream(Flux<RTDEnvelope> marketDocumentStream) {
        marketDocumentStream.subscribe(rtdSink::tryEmitNext);
    }

    public Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentStream() {
        return rtdSink.asFlux();
    }

    @Override
    public void close() {
        rtdSink.tryEmitComplete();
    }
}
