package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

@Service
public class AiidaRegionConnectorService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnectorService.class);
    private final AiidaConfiguration configuration;
    private final Sinks.Many<ConnectionStatusMessage> statusMessageSink = Sinks.many().multicast().onBackpressureBuffer();

    @Autowired
    public AiidaRegionConnectorService(AiidaConfiguration configuration) {
        this.configuration = configuration;
    }

    public Publisher<ConnectionStatusMessage> connectionStatusMessageFlux() {
        return statusMessageSink.asFlux();
    }

    @Override
    public void close() {
        statusMessageSink.tryEmitComplete();
    }
}
