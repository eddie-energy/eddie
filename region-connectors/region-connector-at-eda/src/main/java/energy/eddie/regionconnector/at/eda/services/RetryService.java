package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RetryService {
    private static final Map<PermissionProcessStatus, PermissionProcessStatus> RETRY_AND_FOLLOW_UP_STATES = Map.of(
            PermissionProcessStatus.UNABLE_TO_SEND,
            PermissionProcessStatus.VALIDATED,
            PermissionProcessStatus.FAILED_TO_TERMINATE,
            PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION
    );
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryService.class);
    private final AtPermissionRequestRepository repository;
    private final Outbox outbox;

    public RetryService(AtPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    @SuppressWarnings("java:S6857") // Sonar doesn't like cron syntax a placeholders
    @Scheduled(cron = "${region-connector.at.eda.retry:0 0 * * * *}")
    public void retry() {
        LOGGER.info("Retrying to send failed permission requests");
        var permissionRequests = repository.findByStatusIn(RETRY_AND_FOLLOW_UP_STATES.keySet());
        for (var permissionRequest : permissionRequests) {
            var currentStatus = permissionRequest.status();
            var permissionId = permissionRequest.permissionId();
            var status = RETRY_AND_FOLLOW_UP_STATES.get(currentStatus);
            if (status == null) {
                LOGGER.error("Got invalid permission request {} with status {}", permissionId, currentStatus);
                continue;
            }
            LOGGER.info("Retrying permission request {} with status {} -> {}", permissionId, currentStatus, status);
            outbox.commit(new SimpleEvent(permissionId, status));
        }
    }
}
