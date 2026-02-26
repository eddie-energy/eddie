// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.connectors.cim.v1_12;

import energy.eddie.api.v1_12.outbound.AcknowledgementMarketDocumentOutboundConnector;
import energy.eddie.api.v1_12.outbound.MinMaxEnvelopeOutboundConnector;
import energy.eddie.api.v1_12.outbound.NearRealTimeDataMarketDocumentOutboundConnectorV1_12;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component(value = "cimConnectorV1_12")
@SuppressWarnings("java:S6830")
public class CimConnector implements
        NearRealTimeDataMarketDocumentOutboundConnectorV1_12,
        AcknowledgementMarketDocumentOutboundConnector,
        MinMaxEnvelopeOutboundConnector,
        AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimConnector.class);
    private final Sinks.Many<RTDEnvelope> rtdSink = Sinks.many()
                                                         .replay()
                                                         .limit(Duration.ofSeconds(10));
    private final Sinks.Many<AcknowledgementEnvelope> ackSink = Sinks.many()
                                                                     .replay()
                                                                     .limit(Duration.ofSeconds(10));
    private final Sinks.Many<RECMMOEEnvelope> minMaxEnvelopeSink = Sinks.many()
                                                                        .multicast()
                                                                        .onBackpressureBuffer();

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

    public Flux<AcknowledgementEnvelope> getAcknowledgementMarketDocumentStream() {
        return ackSink.asFlux();
    }

    @Override
    public void setAcknowledgementMarketDocumentStream(Flux<AcknowledgementEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing acknowledgement market document",
                        err
                ))
                .subscribe(ackSink::tryEmitNext);
    }

    @Override
    public Flux<RECMMOEEnvelope> getMinMaxEnvelopes() {
        return minMaxEnvelopeSink.asFlux();
    }

    public void publish(RECMMOEEnvelope minMaxEnvelope) {
        minMaxEnvelopeSink.tryEmitNext(minMaxEnvelope);
    }

    @Override
    public void close() {
        rtdSink.tryEmitComplete();
        ackSink.tryEmitComplete();
        minMaxEnvelopeSink.tryEmitComplete();
    }
}
