package energy.eddie.outbound.rest.connectors.cim.v1_12;

import energy.eddie.api.v1_12.outbound.NearRealTimeDataMarketDocumentOutboundConnectorV1_12;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component(value = "cimConnectorV1_12")
@SuppressWarnings("java:S6830")
public class CimConnector implements NearRealTimeDataMarketDocumentOutboundConnectorV1_12, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimConnector.class);
    private final Sinks.Many<RTDEnvelope> rtdSink = Sinks.many()
                                                         .replay()
                                                         .limit(Duration.ofSeconds(10));

    public Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentStream() {
        return rtdSink.asFlux();
    }

    @Override
    public void setNearRealTimeDataMarketDocumentStreamV1_12(Flux<RTDEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing near real-time data market document",
                        err
                ))
                .subscribe(rtdSink::tryEmitNext);
    }

    @Override
    public void close() {
        rtdSink.tryEmitComplete();
    }
}
