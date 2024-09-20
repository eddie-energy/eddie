package energy.eddie.regionconnector.es.datadis.providers.v0;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class DatadisConnectionStatusMessageProvider implements ConnectionStatusMessageProvider {
    private final Sinks.Many<ConnectionStatusMessage> csm;

    public DatadisConnectionStatusMessageProvider(Sinks.Many<ConnectionStatusMessage> csm) {this.csm = csm;}

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return csm.asFlux();
    }

    @Override
    public void close() throws Exception {
        csm.tryEmitComplete();
    }
}
