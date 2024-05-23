package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.events.DKValidatedEvent;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RetryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryService.class);
    private final DkPermissionRequestRepository repository;
    private final Outbox outbox;

    public RetryService(
            DkPermissionRequestRepository repository,
            Outbox outbox
    ) {
        this.repository = repository;
        this.outbox = outbox;
    }

    @Scheduled(cron = "${region-connector.dk.energinet.retry:0 0 17 * * *}", zone = "Europe/Copenhagen")
    public void retry() {
        LOGGER.info("Retrying sending permission requests to energinet");
        var unableToSend = repository.findAllByStatus(PermissionProcessStatus.UNABLE_TO_SEND);
        for (var pr : unableToSend) {
            var permissionId = pr.permissionId();
            LOGGER.info("Retrying to send permission request {} to energinet", permissionId);
            outbox.commit(new DKValidatedEvent(permissionId, pr.granularity(), pr.start(), pr.end()));
        }
    }
}
