package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.regionconnector.de.eta.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.events.StartPollingEvent;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.streams.AccountingPointDataStream;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedHandler.class);
    private final DeEtaPermissionRequestRepository repo;
    private final DeEtaMdaApiClient api;
    private final ValidatedHistoricalDataStream vhdStream;
    private final AccountingPointDataStream apdStream;
    private final Outbox outbox;

    public AcceptedHandler(
            Flux<PermissionEvent> eventBus,
            DeEtaPermissionRequestRepository repo,
            DeEtaMdaApiClient api,
            ValidatedHistoricalDataStream vhdStream,
            AccountingPointDataStream apdStream,
            Outbox outbox
    ) {
        this.repo = repo;
        this.api = api;
        this.vhdStream = vhdStream;
        this.apdStream = apdStream;
        this.outbox = outbox;

        eventBus.filter(AcceptedEvent.class::isInstance)
                .subscribe(this::accept);
        eventBus.filter(StartPollingEvent.class::isInstance)
                .subscribe(this::accept);
    }

    public void accept(PermissionEvent event) {
        repo.findByPermissionId(event.permissionId()).ifPresent(pr -> {
            api.fetchValidatedHistoricalData(pr)
                    .subscribe(
                            result -> vhdStream.publish(pr, result),
                            error -> handleError(error, event.permissionId())
                    );
            if (event instanceof AcceptedEvent) {
                api.fetchAccountingPointData(pr)
                        .subscribe(
                                result -> apdStream.publish(pr, result),
                                error -> handleError(error, event.permissionId())
                        );
            }
        });
        LOGGER.info("Processing accepted/polling event for ID: {}", event.permissionId());
    }

    private void handleError(Throwable error, String permissionId) {
        if (error instanceof HttpClientErrorException.Forbidden) {
            outbox.commit(new RevokedEvent(permissionId));
        } else {
            throw new RuntimeException(error);
        }
    }
}