package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.events.EsValidatedEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RetryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryService.class);
    private final Outbox outbox;
    private final EsPermissionRequestRepository repository;

    public RetryService(Outbox outbox, EsPermissionRequestRepository repository) {
        this.outbox = outbox;
        this.repository = repository;
    }

    @SuppressWarnings("java:S6857") // Sonar and cron syntax don't play together
    @Scheduled(cron = "${region-connector.es.datadis.retry:0 0 * * * *}")
    public void retry() {
        LOGGER.info("Retrying failed permission requests");
        var permissionRequests = repository.findByStatus(PermissionProcessStatus.UNABLE_TO_SEND);
        for (EsPermissionRequest pr : permissionRequests) {
            var permissionId = pr.permissionId();
            LOGGER.info("Retrying permission request {}", permissionId);
            outbox.commit(new EsValidatedEvent(permissionId, pr.start(), pr.end(), pr.allowedGranularity()));
        }
    }
}
