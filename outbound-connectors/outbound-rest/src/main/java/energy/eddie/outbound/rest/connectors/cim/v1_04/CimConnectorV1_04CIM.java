package energy.eddie.outbound.rest.connectors.cim.v1_04;

import energy.eddie.api.v1_04.outbound.NearRealTimeDataMarketDocumentOutboundConnector;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
@SuppressWarnings("java:S101") // Names shouldn't contain underscores, but this is required to not have bean name clashes with the other CimConnector
public class CimConnectorV1_04CIM implements NearRealTimeDataMarketDocumentOutboundConnector, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimConnectorV1_04CIM.class);
    private final Sinks.Many<RTDEnvelope> rtdSink = CimConnector.createSink();

    @Override
    public void setNearRealTimeDataMarketDocumentStream(Flux<RTDEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing near real-time data market document",
                        err))
                .subscribe(rtdSink::tryEmitNext);
    }

    public Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentStream() {
        return rtdSink.asFlux();
    }

    @Override
    public void close() {
        rtdSink.tryEmitComplete();
    }
}
