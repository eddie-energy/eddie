package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;

/**
 * Event handler for accepted permission requests.
 * When a permission request is accepted, this handler fetches the validated historical data
 * from the ETA Plus API and publishes it to the ValidatedHistoricalDataStream.
 */
@Component
public class AcceptedHandler implements EventHandler<AcceptedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedHandler.class);

    private final DePermissionRequestRepository repository;
    private final EtaPlusApiClient apiClient;
    private final ValidatedHistoricalDataStream stream;
    private final Outbox outbox;

    public AcceptedHandler(
            EventBus eventBus,
            DePermissionRequestRepository repository,
            EtaPlusApiClient apiClient,
            ValidatedHistoricalDataStream stream,
            Outbox outbox
    ) {
        this.repository = repository;
        this.apiClient = apiClient;
        this.stream = stream;
        this.outbox = outbox;
        eventBus.filteredFlux(AcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(AcceptedEvent event) {
        var optionalPr = repository.findByPermissionId(event.permissionId());
        if (optionalPr.isEmpty()) {
            LOGGER.warn("Permission request not found for id: {}", event.permissionId());
            return;
        }

        var pr = optionalPr.get();

        if (pr.start().isAfter(LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID))) {
            LOGGER.atInfo()
                  .addArgument(pr::permissionId)
                  .log("Permission request {} is for future data only, skipping historical data fetch");
            return;
        }

        fetchAndPublishData(pr);
    }

    private void fetchAndPublishData(DePermissionRequest pr) {
        apiClient.fetchMeteredData(pr)
                .subscribe(
                        data -> {
                            LOGGER.atInfo()
                                  .addArgument(pr::permissionId)
                                  .log("Successfully fetched metered data for permission request {}");
                            stream.publish(pr, data);
                        },
                        error -> handleError(error, pr.permissionId())
                );
    }

    private void handleError(Throwable error, String permissionId) {
        if (error instanceof HttpClientErrorException.Forbidden) {
            LOGGER.atWarn()
                  .addArgument(permissionId)
                  .log("Permission {} appears to be revoked, emitting RevokedEvent");
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
        } else {
            LOGGER.atError()
                  .addArgument(permissionId)
                  .addArgument(error::getMessage)
                  .log("Error fetching metered data for permission request {}: {}");
        }
    }
}

