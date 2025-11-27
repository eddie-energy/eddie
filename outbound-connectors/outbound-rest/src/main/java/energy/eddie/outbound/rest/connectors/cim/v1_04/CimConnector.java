// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.connectors.cim.v1_04;

import energy.eddie.api.v1_04.outbound.NearRealTimeDataMarketDocumentOutboundConnectorV1_04;
import energy.eddie.api.v1_04.outbound.ValidatedHistoricalDataMarketDocumentOutboundConnector;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component(value = "cimConnectorV1_04")
@SuppressWarnings("java:S6830")
public class CimConnector implements ValidatedHistoricalDataMarketDocumentOutboundConnector, NearRealTimeDataMarketDocumentOutboundConnectorV1_04, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimConnector.class);
    private final Sinks.Many<VHDEnvelope> vhdSink = Sinks.many()
                                                         .replay()
                                                         .limit(Duration.ofSeconds(10));
    private final Sinks.Many<RTDEnvelope> rtdSink = Sinks.many()
                                                         .replay()
                                                         .limit(Duration.ofSeconds(10));

    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentStream() {
        return vhdSink.asFlux();
    }


    @Override
    public void setValidatedHistoricalDataMarketDocumentStream(Flux<VHDEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing validated historical data market document",
                        err
                ))
                .subscribe(vhdSink::tryEmitNext);
    }

    public Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentStream() {
        return rtdSink.asFlux();
    }

    @Override
    public void setNearRealTimeDataMarketDocumentStreamV1_04(Flux<RTDEnvelope> marketDocumentStream) {
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

