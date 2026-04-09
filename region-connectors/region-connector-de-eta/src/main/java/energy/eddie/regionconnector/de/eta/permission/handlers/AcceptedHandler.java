package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusOperationExceptions.RateLimitException;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
    private final ObservationRegistry observationRegistry;

    public AcceptedHandler(
            EventBus eventBus,
            DePermissionRequestRepository repository,
            EtaPlusApiClient apiClient,
            ValidatedHistoricalDataStream stream,
            Outbox outbox,
            ObservationRegistry observationRegistry
    ) {
        this.repository = repository;
        this.apiClient = apiClient;
        this.stream = stream;
        this.outbox = outbox;
        this.observationRegistry = observationRegistry;
        eventBus.filteredFlux(AcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(AcceptedEvent event) {
        Observation observation = Observation.createNotStarted("de-eta.accepted-handler.process", observationRegistry)
                .highCardinalityKeyValue("permissionId", event.permissionId());
        observation.start();

        try {
            var pr = repository.findByPermissionId(event.permissionId())
                    .orElse(null);

            if (pr == null) {
                LOGGER.atWarn()
                      .addArgument(event::permissionId)
                      .log("Permission request not found for id: {}, skipping event");
                observation.stop();
                return;
            }

            if (pr.start().isAfter(LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID))) {
                LOGGER.atInfo()
                      .addArgument(pr::permissionId)
                      .log("Permission request {} is for future data only, skipping historical data fetch");
                observation.stop();
                return;
            }

            fetchAndPublishData(pr, event.accessToken(), observation);
        } catch (Exception e) {
            observation.error(e);
            observation.stop();
            LOGGER.atError()
                  .addArgument(event::permissionId)
                  .setCause(e)
                  .log("Fatal error processing AcceptedEvent for permission {}");
            commitSafely(event.permissionId(), PermissionProcessStatus.UNABLE_TO_SEND);
        }
    }

    private void fetchAndPublishData(DePermissionRequest pr, String accessToken, Observation observation) {
        apiClient.fetchMeteredData(pr, accessToken)
                .doOnError(observation::error)
                .doFinally(signal -> observation.stop())
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
        if (error instanceof RateLimitException) {
            LOGGER.atWarn()
                  .addArgument(permissionId)
                  .log("Rate limit exceeded for permission request {}, marking as UNABLE_TO_SEND for retry");
            commitSafely(permissionId, PermissionProcessStatus.UNABLE_TO_SEND);
        } else if (error instanceof WebClientResponseException.Forbidden) {
            LOGGER.atWarn()
                  .addArgument(permissionId)
                  .log("Permission {} appears to be revoked, emitting RevokedEvent");
            commitSafely(permissionId, PermissionProcessStatus.REVOKED);
        } else {
            LOGGER.atError()
                  .addArgument(permissionId)
                  .addArgument(error::getMessage)
                  .setCause(error)
                  .log("Error fetching metered data for permission request {}: {}");
            commitSafely(permissionId, PermissionProcessStatus.UNABLE_TO_SEND);
        }
    }

    private void commitSafely(String permissionId, PermissionProcessStatus status) {
        try {
            outbox.commit(new SimpleEvent(permissionId, status));
        } catch (Exception ex) {
            LOGGER.atError()
                  .addArgument(status)
                  .addArgument(permissionId)
                  .setCause(ex)
                  .log("Failed to persist {} event for permission {}");
        }
    }
}