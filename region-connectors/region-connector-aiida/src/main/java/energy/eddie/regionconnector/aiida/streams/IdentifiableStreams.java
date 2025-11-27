// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.streams;

import energy.eddie.api.agnostic.RawDataMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class IdentifiableStreams {
    private final Sinks.Many<energy.eddie.cim.v1_04.rtd.RTDEnvelope> nearRealTimeDataCimV104Sink;
    private final Sinks.Many<energy.eddie.cim.v1_06.rtd.RTDEnvelope> nearRealTimeDataCimV106Sink;
    private final Sinks.Many<RawDataMessage> rawDataMessageSink;

    public IdentifiableStreams(
            Sinks.Many<energy.eddie.cim.v1_04.rtd.RTDEnvelope> nearRealTimeDataCimV104Sink,
            Sinks.Many<energy.eddie.cim.v1_06.rtd.RTDEnvelope> nearRealTimeDataCimV106Sink,
            Sinks.Many<RawDataMessage> rawDataMessageSink
    ) {
        this.nearRealTimeDataCimV104Sink = nearRealTimeDataCimV104Sink;
        this.nearRealTimeDataCimV106Sink = nearRealTimeDataCimV106Sink;
        this.rawDataMessageSink = rawDataMessageSink;
    }

    public Flux<energy.eddie.cim.v1_04.rtd.RTDEnvelope> nearRealTimeDataCimV104Flux() {
        return nearRealTimeDataCimV104Sink.asFlux();
    }

    public Flux<energy.eddie.cim.v1_06.rtd.RTDEnvelope> nearRealTimeDataCimV106Flux() {
        return nearRealTimeDataCimV106Sink.asFlux();
    }

    public Flux<RawDataMessage> rawDataMessageFlux() {
        return rawDataMessageSink.asFlux();
    }
}
