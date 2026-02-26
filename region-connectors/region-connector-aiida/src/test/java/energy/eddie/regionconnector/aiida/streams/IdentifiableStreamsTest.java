// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0


package energy.eddie.regionconnector.aiida.streams;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import org.junit.jupiter.api.BeforeEach;
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

    private Sinks.Many<energy.eddie.cim.v1_04.rtd.RTDEnvelope> rtdCimV104Sink;
    private Sinks.Many<energy.eddie.cim.v1_12.rtd.RTDEnvelope> rtdCimV112Sink;
    private Sinks.Many<AcknowledgementEnvelope> ackSink;
    private Sinks.Many<RawDataMessage> rawSink;
    private IdentifiableStreams streams;

    @BeforeEach
    void setUp() {
        rtdCimV104Sink = Sinks.many().unicast().onBackpressureBuffer();
        rtdCimV112Sink = Sinks.many().unicast().onBackpressureBuffer();
        ackSink = Sinks.many().unicast().onBackpressureBuffer();
        rawSink = Sinks.many().unicast().onBackpressureBuffer();
        streams = new IdentifiableStreams(rtdCimV104Sink, rtdCimV112Sink, ackSink, rawSink);
    }

    @Test
    void nearRealTimeDataCimV112Flux_forwardsEmittedEnvelopes() {
        var one = new energy.eddie.cim.v1_12.rtd.RTDEnvelope();
        var two = new energy.eddie.cim.v1_12.rtd.RTDEnvelope();

        StepVerifier.create(streams.nearRealTimeDataCimV112Flux())
                    .then(() -> {
                        rtdCimV112Sink.tryEmitNext(one);
                        rtdCimV112Sink.tryEmitNext(two);
                    })
                    .expectNext(one, two)
                    .thenCancel()
                    .verify();
    }

    @Test
    void nearRealTimeDataCimV104Flux_forwardsEmittedEnvelopes() {
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
    void acknowledgementFlux_forwardsEmittedEnvelopes() {
        var one = new AcknowledgementEnvelope();
        var two = new AcknowledgementEnvelope();

        StepVerifier.create(streams.acknowledgementCimFlux())
                    .then(() -> {
                        ackSink.tryEmitNext(one);
                        ackSink.tryEmitNext(two);
                    })
                    .expectNext(one, two)
                    .thenCancel()
                    .verify();
    }

    @Test
    void rawDataMessageFlux_forwardsEmittedMessages() {
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
