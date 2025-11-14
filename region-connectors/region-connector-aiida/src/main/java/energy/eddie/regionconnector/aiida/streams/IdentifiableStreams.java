package energy.eddie.regionconnector.aiida.streams;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class IdentifiableStreams {
    private final Sinks.Many<RTDEnvelope> nearRealTimeDataSink;
    private final Sinks.Many<RawDataMessage> rawDataMessageSink;

    public IdentifiableStreams(
            Sinks.Many<RTDEnvelope> nearRealTimeDataSink,
            Sinks.Many<RawDataMessage> rawDataMessageSink
    ) {
        this.nearRealTimeDataSink = nearRealTimeDataSink;
        this.rawDataMessageSink = rawDataMessageSink;
    }

    public Flux<RTDEnvelope> nearRealTimeDataFlux() {
        return nearRealTimeDataSink.asFlux();
    }

    public Flux<RawDataMessage> rawDataMessageFlux() {
        return rawDataMessageSink.asFlux();
    }
}
