package energy.eddie.outbound.rest.connectors;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.outbound.ConnectionStatusMessageOutboundConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component
public class AgnosticConnector implements ConnectionStatusMessageOutboundConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgnosticConnector.class);
    private final Sinks.Many<ConnectionStatusMessage> csmSink = Sinks.many()
                                                                     .replay()
                                                                     .limit(Duration.ofSeconds(10));

    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return csmSink.asFlux();
    }

    @Override
    public void setConnectionStatusMessageStream(Flux<ConnectionStatusMessage> connectionStatusMessageStream) {
        connectionStatusMessageStream
                .onErrorContinue((err, obj) -> LOGGER.warn("Got error while processing connection status", err))
                .subscribe(csmSink::tryEmitNext);
    }
}
