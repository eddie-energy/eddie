// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.connectors;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.agnostic.outbound.OpaqueEnvelopeOutboundConnector;
import energy.eddie.api.agnostic.outbound.RawDataOutboundConnector;
import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.agnostic.RawDataMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component
public class AgnosticConnector implements
        RawDataOutboundConnector,
        OpaqueEnvelopeOutboundConnector,
        AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgnosticConnector.class);
    private final Sinks.Many<ConnectionStatusMessage> csmSink = Sinks.many()
                                                                     .replay()
                                                                     .limit(Duration.ofSeconds(10));
    private final Sinks.Many<RawDataMessage> rdSink = Sinks.many()
                                                           .replay()
                                                           .limit(Duration.ofSeconds(10));
    private final Sinks.Many<OpaqueEnvelope> opaqueEnvelopeSink = Sinks.many()
                                                                       .multicast()
                                                                       .onBackpressureBuffer();

    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return csmSink.asFlux();
    }

    public Flux<RawDataMessage> getRawDataMessageStream() {
        return rdSink.asFlux();
    }


    @MessageStream(ConnectionStatusMessage.class)
    public void setConnectionStatusMessageStream(Flux<ConnectionStatusMessage> connectionStatusMessageStream) {
        connectionStatusMessageStream
                .onErrorContinue((err, obj) -> LOGGER.warn("Got error while processing connection status", err))
                .subscribe(csmSink::tryEmitNext);
    }

    @Override
    public void setRawDataStream(Flux<RawDataMessage> rawDataStream) {
        rawDataStream
                .onErrorContinue((err, obj) -> LOGGER.warn("Got error while processing raw data", err))
                .subscribe(rdSink::tryEmitNext);
    }

    @Override
    public Flux<OpaqueEnvelope> getOpaqueEnvelopes() {
        return opaqueEnvelopeSink.asFlux();
    }

    public void publish(OpaqueEnvelope envelope) {
        opaqueEnvelopeSink.tryEmitNext(envelope);
    }

    @Override
    public void close() {
        csmSink.tryEmitComplete();
        rdSink.tryEmitComplete();
        opaqueEnvelopeSink.tryEmitComplete();
    }
}
