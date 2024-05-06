package energy.eddie.regionconnector.es.datadis.providers.v0;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class DatadisMvp1ConnectionStatusMessageProvider implements Mvp1ConnectionStatusMessageProvider {
    private final Sinks.Many<ConnectionStatusMessage> csm;

    public DatadisMvp1ConnectionStatusMessageProvider(Sinks.Many<ConnectionStatusMessage> csm) {this.csm = csm;}

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return csm.asFlux();
    }

    @Override
    public void close() throws Exception {
        csm.tryEmitComplete();
    }
}
