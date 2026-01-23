// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

// java
package energy.eddie.regionconnector.aiida.streams;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
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
    void nearRealTimeDataFlux_forwardsEmittedEnvelopes() {
        Sinks.Many<RTDEnvelope> rtdSink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<RawDataMessage> rawSink = Sinks.many().unicast().onBackpressureBuffer();

        var streams = new IdentifiableStreams(rtdSink, rawSink);

        var one = new RTDEnvelope();
        var two = new RTDEnvelope();

        StepVerifier.create(streams.nearRealTimeDataFlux())
                    .then(() -> {
                        rtdSink.tryEmitNext(one);
                        rtdSink.tryEmitNext(two);
                    })
                    .expectNext(one, two)
                    .thenCancel()
                    .verify();
    }

    @Test
    void rawDataMessageFlux_forwardsEmittedMessages() {
        Sinks.Many<RTDEnvelope> rtdSink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<RawDataMessage> rawSink = Sinks.many().unicast().onBackpressureBuffer();

        var streams = new IdentifiableStreams(rtdSink, rawSink);

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
