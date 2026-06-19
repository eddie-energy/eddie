// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.connectors.cim.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.v1_12.outbound.MinMaxEnvelopeOutboundConnector;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component(value = "cimConnectorV1_12")
@SuppressWarnings("java:S6830")
public class CimConnector implements MinMaxEnvelopeOutboundConnector, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimConnector.class);
    private final Sinks.Many<RTDEnvelope> rtdSink = Sinks.many()
                                                         .replay()
                                                         .limit(Duration.ofSeconds(10));
    private final Sinks.Many<AcknowledgementEnvelope> ackSink = Sinks.many()
                                                                     .replay()
                                                                     .limit(Duration.ofSeconds(10));
    private final Sinks.Many<ESRDMDEnvelope> esrdmdSink = Sinks.many()
                                                               .replay()
                                                               .limit(Duration.ofSeconds(10));
    private final Sinks.Many<RECMMOEEnvelope> minMaxEnvelopeSink = Sinks.many()
                                                                        .multicast()
                                                                        .onBackpressureBuffer();
    private final Sinks.Many<RequestPermissionEnvelope> rpmdSink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentStream() {
        return rtdSink.asFlux();
    }

    @MessageStream(energy.eddie.cim.v1_12.rtd.RTDEnvelope.class)
    @SuppressWarnings("java:S100")
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

    @MessageStream(AcknowledgementEnvelope.class)
    public void setAcknowledgementMarketDocumentStream(Flux<AcknowledgementEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing acknowledgement market document",
                        err
                ))
                .subscribe(ackSink::tryEmitNext);
    }

    public Flux<ESRDMDEnvelope> getEnergySharingReferenceDataMarketDocumentStream() {
        return esrdmdSink.asFlux();
    }

    @MessageStream(ESRDMDEnvelope.class)
    public void setEnergySharingReferenceDataMarketDocumentStream(Flux<ESRDMDEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing energy sharing reference data market document",
                        err
                ))
                .subscribe(esrdmdSink::tryEmitNext);
    }

    @Override
    public Flux<RECMMOEEnvelope> getMinMaxEnvelopes() {
        return minMaxEnvelopeSink.asFlux();
    }

    @MessageStream(RECMMOEEnvelope.class)
    public void setMinMaxEnvelopeStream(Flux<RECMMOEEnvelope> minMaxEnvelopeStream) {
        minMaxEnvelopeStream
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing min-max envelope",
                        err
                ))
                .subscribe(minMaxEnvelopeSink::tryEmitNext);
    }

    public void publish(RECMMOEEnvelope minMaxEnvelope) {
        minMaxEnvelopeSink.tryEmitNext(minMaxEnvelope);
    }

    public Flux<RequestPermissionEnvelope> getRequestPermissionMarketDocumentStream() {
        return rpmdSink.asFlux();
    }

    @MessageStream(RequestPermissionEnvelope.class)
    public void setRequestPermissionMarketDocumentStream(Flux<RequestPermissionEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing request permission market document",
                        err
                ))
                .subscribe(rpmdSink::tryEmitNext);
    }

    @Override
    public void close() {
        rtdSink.tryEmitComplete();
        ackSink.tryEmitComplete();
        minMaxEnvelopeSink.tryEmitComplete();
        esrdmdSink.tryEmitComplete();
        rpmdSink.tryEmitComplete();
    }
}
