package energy.eddie.regionconnector.dk.energinet.providers.v0;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class EnerginetConnectionStatusMessageProvider implements ConnectionStatusMessageProvider {
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;

    public EnerginetConnectionStatusMessageProvider(Sinks.Many<ConnectionStatusMessage> connectionStatusSink) {
        this.connectionStatusSink = connectionStatusSink;
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusSink.asFlux();
    }

    @Override
    public void close() {
        connectionStatusSink.tryEmitComplete();
    }
}
