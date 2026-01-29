// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.connectors.cim.v1_04;

import energy.eddie.api.v1_04.outbound.NearRealTimeDataMarketDocumentOutboundConnector;
import energy.eddie.api.v1_04.outbound.ValidatedHistoricalDataMarketDocumentOutboundConnector;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component
@SuppressWarnings("java:S101")
public class CimConnectorV1_04 implements ValidatedHistoricalDataMarketDocumentOutboundConnector, NearRealTimeDataMarketDocumentOutboundConnector, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimConnectorV1_04.class);
    private final Sinks.Many<VHDEnvelope> vhdSink = Sinks.many()
                                                         .replay()
                                                         .limit(Duration.ofSeconds(10));
    private final Sinks.Many<RTDEnvelope> rtdSink = CimConnector.createSink();

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
    public void setNearRealTimeDataMarketDocumentStream(Flux<RTDEnvelope> marketDocumentStream) {
        marketDocumentStream.subscribe(rtdSink::tryEmitNext);
    }

    @Override
    public void close() {
        rtdSink.tryEmitComplete();
    }
}
