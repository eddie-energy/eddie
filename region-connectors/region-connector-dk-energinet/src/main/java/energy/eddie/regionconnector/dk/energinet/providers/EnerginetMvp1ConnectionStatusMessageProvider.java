package energy.eddie.regionconnector.dk.energinet.providers;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.concurrent.Flow;

@Component
public class EnerginetMvp1ConnectionStatusMessageProvider implements Mvp1ConnectionStatusMessageProvider {
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;

    public EnerginetMvp1ConnectionStatusMessageProvider(Sinks.Many<ConnectionStatusMessage> connectionStatusSink) {
        this.connectionStatusSink = connectionStatusSink;
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(connectionStatusSink.asFlux());
    }

    @Override
    public void close() {
        connectionStatusSink.tryEmitComplete();
    }
}
