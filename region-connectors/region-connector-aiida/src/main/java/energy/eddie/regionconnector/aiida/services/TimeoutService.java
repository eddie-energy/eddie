package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.Timeout;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutService.class);
    private final AiidaPermissionRequestViewRepository permissionRequestRepository;
    private final TimeoutConfiguration timeoutConfig;
    private final Outbox outbox;

    public TimeoutService(
            AiidaPermissionRequestViewRepository permissionRequestRepository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Wired from parent context
            TimeoutConfiguration timeoutConfig,
            Outbox outbox
    ) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.timeoutConfig = timeoutConfig;
        this.outbox = outbox;
    }

    @Timeout
    public void timeout() {
        LOGGER.info("Querying for stale permission requests");
        var stalePermissions = permissionRequestRepository.findStalePermissionRequests(timeoutConfig.duration());
        for (var pr : stalePermissions) {
            var permissionId = pr.permissionId();
            LOGGER.info("Timing permission request {} out", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.TIMED_OUT));
        }
    }
}
