// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

// java
package energy.eddie.regionconnector.aiida.streams;

import energy.eddie.api.agnostic.RawDataMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class IdentifiableStreamsTest {
    @Mock
    RawDataMessage rawMsg1;

    @Mock
    RawDataMessage rawMsg2;

    @Test
    void nearRealTimeDataCimV106Flux_forwardsEmittedEnvelopes() {
        Sinks.Many<energy.eddie.cim.v1_04.rtd.RTDEnvelope> rtdCimV104Sink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<energy.eddie.cim.v1_06.rtd.RTDEnvelope> rtdCimV106Sink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<RawDataMessage> rawSink = Sinks.many().unicast().onBackpressureBuffer();

        var streams = new IdentifiableStreams(rtdCimV104Sink, rtdCimV106Sink, rawSink);

        var one = new energy.eddie.cim.v1_06.rtd.RTDEnvelope();
        var two = new energy.eddie.cim.v1_06.rtd.RTDEnvelope();

        StepVerifier.create(streams.nearRealTimeDataCimV106Flux())
                    .then(() -> {
                        rtdCimV106Sink.tryEmitNext(one);
                        rtdCimV106Sink.tryEmitNext(two);
                    })
                    .expectNext(one, two)
                    .thenCancel()
                    .verify();
    }

    @Test
    void nearRealTimeDataCimV104Flux_forwardsEmittedEnvelopes() {
        Sinks.Many<energy.eddie.cim.v1_04.rtd.RTDEnvelope> rtdCimV104Sink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<energy.eddie.cim.v1_06.rtd.RTDEnvelope> rtdCimV106Sink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<RawDataMessage> rawSink = Sinks.many().unicast().onBackpressureBuffer();

        var streams = new IdentifiableStreams(rtdCimV104Sink, rtdCimV106Sink, rawSink);

        var one = new energy.eddie.cim.v1_04.rtd.RTDEnvelope();
        var two = new energy.eddie.cim.v1_04.rtd.RTDEnvelope();

        StepVerifier.create(streams.nearRealTimeDataCimV104Flux())
                    .then(() -> {
                        rtdCimV104Sink.tryEmitNext(one);
                        rtdCimV104Sink.tryEmitNext(two);
                    })
                    .expectNext(one, two)
                    .thenCancel()
                    .verify();
    }

    @Test
    void rawDataMessageFlux_forwardsEmittedMessages() {
        Sinks.Many<energy.eddie.cim.v1_04.rtd.RTDEnvelope> rtdCimV104Sink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<energy.eddie.cim.v1_06.rtd.RTDEnvelope> rtdCimV106Sink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<RawDataMessage> rawSink = Sinks.many().unicast().onBackpressureBuffer();

        var streams = new IdentifiableStreams(rtdCimV104Sink, rtdCimV106Sink, rawSink);

        StepVerifier.create(streams.rawDataMessageFlux())
                    .then(() -> {
                        rawSink.tryEmitNext(rawMsg1);
                        rawSink.tryEmitNext(rawMsg2);
                    })
                    .expectNext(rawMsg1, rawMsg2)
                    .thenCancel()
                    .verify();
    }
}
