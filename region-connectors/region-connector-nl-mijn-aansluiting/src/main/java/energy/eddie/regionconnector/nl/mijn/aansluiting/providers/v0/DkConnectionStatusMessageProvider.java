package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class DkConnectionStatusMessageProvider implements Mvp1ConnectionStatusMessageProvider {
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessages;

    public DkConnectionStatusMessageProvider(Sinks.Many<ConnectionStatusMessage> connectionStatusMessages) {
        this.connectionStatusMessages = connectionStatusMessages;
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusMessages.asFlux();
    }

    @Override
    public void close() throws Exception {
        // NoOp
    }
}
