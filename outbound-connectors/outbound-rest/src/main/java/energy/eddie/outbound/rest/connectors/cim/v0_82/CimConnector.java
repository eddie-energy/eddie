package energy.eddie.outbound.rest.connectors.cim.v0_82;

import energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component
public class CimConnector implements ValidatedHistoricalDataEnvelopeOutboundConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimConnector.class);
    private final Sinks.Many<ValidatedHistoricalDataEnvelope> vhdSink = Sinks.many()
                                                                             .replay()
                                                                             .limit(Duration.ofSeconds(10));

    @Override
    public void setEddieValidatedHistoricalDataMarketDocumentStream(Flux<ValidatedHistoricalDataEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing validated historical data market document",
                        err))
                .subscribe(vhdSink::tryEmitNext);
    }

    public Flux<ValidatedHistoricalDataEnvelope> getHistoricalDataMarketDocumentStream() {
        return vhdSink.asFlux();
    }
}
