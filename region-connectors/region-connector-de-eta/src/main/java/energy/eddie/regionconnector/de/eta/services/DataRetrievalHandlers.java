package energy.eddie.regionconnector.de.eta.services;

import energy.eddie.regionconnector.de.eta.client.DeEtaMdaApiClient;
import energy.eddie.regionconnector.de.eta.permission.events.*;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Subscribes to Accepted and Retransmit events to fetch data from ETA+/MDA and emit availability events.
 */
@Component
public class DataRetrievalHandlers implements EventHandler<AcceptedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataRetrievalHandlers.class);
    private final EventBus eventBus;
    private final DeEtaPermissionRequestRepository repository;
    private final DeEtaMdaApiClient mdaClient;
    private final Outbox outbox;

    public DataRetrievalHandlers(
            EventBus eventBus,
            DeEtaPermissionRequestRepository repository,
            DeEtaMdaApiClient mdaClient,
            Outbox outbox
    ) {
        this.eventBus = eventBus;
        this.repository = repository;
        this.mdaClient = mdaClient;
        this.outbox = outbox;
        eventBus.filteredFlux(AcceptedEvent.class).subscribe(this::accept);
        eventBus.filteredFlux(RetransmitRequestedEvent.class).subscribe(this::onRetransmit);
    }

    @Override
    public void accept(AcceptedEvent event) {
        var permissionId = event.permissionId();
        var maybe = repository.findByPermissionId(permissionId);
        if (maybe.isEmpty()) {
            LOGGER.warn("Permission request {} not found for data retrieval", permissionId);
            return;
        }
        var pr = maybe.get();

        // Request validated historical data
        mdaClient.fetchValidatedHistoricalData(pr)
                 .doOnError(err -> LOGGER.warn("Failed to fetch validated historical data for {}", permissionId, err))
                 .subscribe(resp -> {
                     // Here we would publish raw data to streams and map to documents (OTA/MDA)
                     outbox.commit(new ValidatedHistoricalDataAvailableEvent(permissionId));
                 });

        // Request accounting point data
        mdaClient.fetchAccountingPointData(pr)
                 .doOnError(err -> LOGGER.warn("Failed to fetch accounting point data for {}", permissionId, err))
                 .subscribe(resp -> {
                     // Here we would publish raw data to streams and map to market documents
                     outbox.commit(new AccountingPointDataAvailableEvent(permissionId));
                 });
    }

    private void onRetransmit(RetransmitRequestedEvent event) {
        // On retransmission, simply re-use the same retrieval logic
        accept(new AcceptedEvent(event.permissionId()));
    }
}
