package energy.eddie.regionconnector.de.eta.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.events.RetryValidatedEvent;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RetryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryService.class);
    private final DeEtaPermissionRequestRepository repository;
    private final Outbox outbox;

    public RetryService(DeEtaPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    @Scheduled(cron = "${region-connector.de.eta.retry:0 0 * * * *}")
    public void retry() {
        var permissionRequests = repository.findByStatus(PermissionProcessStatus.UNABLE_TO_SEND);
        for (var permissionRequest : permissionRequests) {
            LOGGER.info("Retrying permission request {}", permissionRequest.permissionId());
            outbox.commit(new RetryValidatedEvent(permissionRequest.permissionId()));
        }
    }
}
