package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.providers.AccountingPointDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AccountingPointDataHandler implements EventHandler<AcceptedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointDataHandler.class);

    private final DePermissionRequestRepository repository;
    private final DataNeedsService dataNeedsService;
    private final EtaPlusApiClient apiClient;
    private final AccountingPointDataStream stream;

    public AccountingPointDataHandler(
            EventBus eventBus,
            DePermissionRequestRepository repository,
            DataNeedsService dataNeedsService,
            EtaPlusApiClient apiClient,
            AccountingPointDataStream stream
    ) {
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
        this.apiClient = apiClient;
        this.stream = stream;
        eventBus.filteredFlux(AcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(AcceptedEvent event) {
        Optional<DePermissionRequest> optionalPr = repository.findByPermissionId(event.permissionId());

        if (optionalPr.isEmpty()) {
            LOGGER.warn("Permission request not found for id: {}", event.permissionId());
            return;
        }

        DePermissionRequest pr = optionalPr.get();

        // Check if this is an accounting point data need
        DataNeed dataNeed = dataNeedsService.getById(pr.dataNeedId());
        if (!(dataNeed instanceof AccountingPointDataNeed)) {
            // Not an accounting point data need, skip
            return;
        }

        LOGGER.info("Fetching accounting point data for permission {} and metering point {}", 
                pr.permissionId(), pr.meteringPointId());

        // Fetch accounting point data from MDA
        apiClient.fetchAccountingPointData(pr.meteringPointId())
                .subscribe(
                        accountingPointData -> {
                            LOGGER.info("Successfully fetched accounting point data for permission {}", 
                                    pr.permissionId());
                            // Publish to stream for raw data and CIM document processing
                            stream.publish(pr, accountingPointData);
                        },
                        error -> {
                            LOGGER.error("Failed to fetch accounting point data for permission {}", 
                                    pr.permissionId(), error);
                            // Error handling: could emit a revocation event if the error indicates
                            // the customer revoked the permission, similar to validated historical data
                        }
                );
    }
}
