package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.Timeout;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutService.class);
    private final NlPermissionRequestRepository repository;
    private final Outbox outbox;
    private final TimeoutConfiguration config;

    public TimeoutService(
            NlPermissionRequestRepository repository,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Injected from parent context
            TimeoutConfiguration config
    ) {
        this.repository = repository;
        this.outbox = outbox;
        this.config = config;
    }

    @Timeout
    public void timeout() {
        LOGGER.info("Querying permission requests to timeout");
        var permissionRequests = repository.findStalePermissionRequests(config.duration());
        for (var pr : permissionRequests) {
            var permissionId = pr.permissionId();
            LOGGER.info("Timing out permission request {}", permissionId);
            outbox.commit(new NlSimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
            outbox.commit(new NlSimpleEvent(permissionId, PermissionProcessStatus.TIMED_OUT));
        }
    }
}
