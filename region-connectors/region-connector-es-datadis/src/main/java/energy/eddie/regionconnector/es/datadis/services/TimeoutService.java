package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.Timeout;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutService.class);

    private final EsPermissionRequestRepository repository;
    private final Outbox outbox;
    private final TimeoutConfiguration timeoutConfiguration;

    public TimeoutService(
            EsPermissionRequestRepository repository,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Injected from parent context
            TimeoutConfiguration timeoutConfiguration
    ) {
        this.repository = repository;
        this.outbox = outbox;
        this.timeoutConfiguration = timeoutConfiguration;
    }

    @Timeout
    public void timeout() {
        LOGGER.info("Checking for stale permission requests");
        var toTimeout = repository.findStalePermissionRequests(timeoutConfiguration.duration());
        for (var pr : toTimeout) {
            var permissionId = pr.permissionId();
            LOGGER.info("Timing permission request {} out", permissionId);
            outbox.commit(new EsSimpleEvent(permissionId, PermissionProcessStatus.TIMED_OUT));
        }
    }
}
