package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.Timeout;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutService.class);
    private final FrPermissionRequestRepository repository;
    private final Outbox outbox;
    private final TimeoutConfiguration timeoutConfig;

    public TimeoutService(
            FrPermissionRequestRepository repository,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Autowired from core
            TimeoutConfiguration timeoutConfiguration
    ) {
        this.repository = repository;
        this.outbox = outbox;
        this.timeoutConfig = timeoutConfiguration;
    }

    @Timeout
    public void timeoutPendingPermissionRequests() {
        LOGGER.info("Querying permission requests to timeout.");
        var permissionRequests = repository.findTimedOutPermissionRequests(timeoutConfig.duration());
        LOGGER.info(
                "Found {} permission requests that have been stale for the last {} hours, starting timeout.",
                permissionRequests.size(),
                timeoutConfig.duration()
        );
        for (var pr : permissionRequests) {
            var permissionId = pr.permissionId();
            LOGGER.info("Timing out permission request {}", permissionId);
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.TIMED_OUT));
        }
    }
}
