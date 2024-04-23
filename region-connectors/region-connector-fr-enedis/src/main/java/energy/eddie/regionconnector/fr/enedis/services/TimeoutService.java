package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutService.class);
    private final EnedisConfiguration configuration;
    private final FrPermissionRequestRepository repository;
    private final Outbox outbox;

    public TimeoutService(
            EnedisConfiguration configuration,
            FrPermissionRequestRepository repository,
            Outbox outbox
    ) {
        this.configuration = configuration;
        this.repository = repository;
        this.outbox = outbox;
    }

    @SuppressWarnings("java:S6857") // Sonar thinks that the cron placeholder is malformed
    @Scheduled(cron = "${region-connector.fr.enedis.timeout.schedule:0 0 * * * *}")
    public void timeoutPendingPermissionRequests() {
        LOGGER.info("Querying permission requests to timeout.");
        var permissionRequests = repository.findTimedOutPermissionRequests(configuration.timeoutDuration());
        LOGGER.info("Found {} permission requests to timeout", permissionRequests.size());
        for (var pr : permissionRequests) {
            var permissionId = pr.permissionId();
            LOGGER.info("Timing out permission request {}", permissionId);
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.TIMED_OUT));
        }
    }
}
