package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.regionconnector.de.eta.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.streams.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.de.eta.client.DeEtaMdaApiClient;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.de.eta.permission.events.RevokedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;

@Component
public class AcceptedHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedEventHandler.class);
    private final DeEtaPermissionRequestRepository repo;
    private final DeEtaMdaApiClient api;
    private final ValidatedHistoricalDataStream stream;
    private final Outbox outbox;

    public AcceptedHandler(
            Flux<PermissionEvent> eventBus,
            DeEtaPermissionRequestRepository repo,
            DeEtaMdaApiClient api,
            ValidatedHistoricalDataStream stream,
            Outbox outbox
    ) {
        this.repo = repo;
        this.api = api;
        this.stream = stream;
        this.outbox = outbox;
        eventBus.filter(AcceptedEvent.class::isInstance)
                .subscribe(this::accept);
    }

    public void accept(PermissionEvent event) {
        repo.findByPermissionId(event.permissionId()).ifPresent(pr ->
                api.fetchValidatedHistoricalData(pr)
                    .subscribe(
                            result -> stream.publish(pr, result),
                            error -> handleError(error, event.permissionId())
                    )
        );
        LOGGER.info("The Streaming of got accepted.");
    }

    private void handleError(Throwable error, String permissionId) {
        if (error instanceof HttpClientErrorException.Forbidden) {
            outbox.commit(new RevokedEvent(permissionId));
        } else {
            throw new RuntimeException(error);
        }
    }
}
