package energy.eddie.regionconnector.dk.energinet.providers.v0;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class EnerginetMvp1ConnectionStatusMessageProvider implements Mvp1ConnectionStatusMessageProvider {
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;

    public EnerginetMvp1ConnectionStatusMessageProvider(Sinks.Many<ConnectionStatusMessage> connectionStatusSink) {
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
