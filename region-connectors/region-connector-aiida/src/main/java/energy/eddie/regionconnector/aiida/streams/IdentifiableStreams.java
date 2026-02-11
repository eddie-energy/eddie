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
    private final Sinks.Many<energy.eddie.cim.v1_12.rtd.RTDEnvelope> nearRealTimeDataCimV112Sink;
    private final Sinks.Many<RawDataMessage> rawDataMessageSink;

    public IdentifiableStreams(
            Sinks.Many<energy.eddie.cim.v1_04.rtd.RTDEnvelope> nearRealTimeDataCimV104Sink,
            Sinks.Many<energy.eddie.cim.v1_12.rtd.RTDEnvelope> nearRealTimeDataCimV112Sink,
            Sinks.Many<RawDataMessage> rawDataMessageSink
    ) {
        this.nearRealTimeDataCimV104Sink = nearRealTimeDataCimV104Sink;
        this.nearRealTimeDataCimV112Sink = nearRealTimeDataCimV112Sink;
        this.rawDataMessageSink = rawDataMessageSink;
    }

    public Flux<energy.eddie.cim.v1_04.rtd.RTDEnvelope> nearRealTimeDataCimV104Flux() {
        return nearRealTimeDataCimV104Sink.asFlux();
    }

    public Flux<energy.eddie.cim.v1_12.rtd.RTDEnvelope> nearRealTimeDataCimV112Flux() {
        return nearRealTimeDataCimV112Sink.asFlux();
    }

    public Flux<RawDataMessage> rawDataMessageFlux() {
        return rawDataMessageSink.asFlux();
    }
}
