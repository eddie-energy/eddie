package energy.eddie.core.services;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class PermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink = Sinks.many()
                                                                                         .multicast()
                                                                                         .onBackpressureBuffer();

    public void registerProvider(ConnectionStatusMessageProvider statusMessageProvider) {
        LOGGER.info("PermissionService: Registering {}", statusMessageProvider.getClass().getName());
        statusMessageProvider.getConnectionStatusMessageStream()
                             .doOnNext(connectionStatusMessageSink::tryEmitNext)
                             .doOnError(connectionStatusMessageSink::tryEmitError)
                             .subscribe();
    }

    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusMessageSink.asFlux();
    }
}
