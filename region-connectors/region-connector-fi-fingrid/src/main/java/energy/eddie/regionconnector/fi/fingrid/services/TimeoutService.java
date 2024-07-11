package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.Timeout;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutService.class);
    private final FiPermissionRequestRepository repository;
    private final TimeoutConfiguration config;
    private final Outbox outbox;

    public TimeoutService(
            FiPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Injected from parent context
            TimeoutConfiguration config,
            Outbox outbox
    ) {
        this.repository = repository;
        this.config = config;
        this.outbox = outbox;
    }

    @Timeout
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void timeout() {
        LOGGER.info("Querying permission requests to timeout");
        var permissionRequests = repository.findStalePermissionRequests(config.duration());
        for (var pr : permissionRequests) {
            var permissionId = pr.permissionId();
            LOGGER.info("Timing out permission request {}", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.TIMED_OUT));
        }
    }
}
