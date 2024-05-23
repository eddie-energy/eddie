package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutService.class);

    private final EsPermissionRequestRepository repository;
    private final Outbox outbox;
    private final DatadisConfig config;

    public TimeoutService(EsPermissionRequestRepository repository, Outbox outbox, DatadisConfig config) {
        this.repository = repository;
        this.outbox = outbox;
        this.config = config;
    }

    @Scheduled(cron = "${region-connector.es.datadis.timeout.schedule:0 0 * * * *}")
    public void timeout() {
        LOGGER.info("Checking for stale permission requests");
        var toTimeout = repository.findStalePermissionRequests(config.timeoutDuration());
        for (var pr : toTimeout) {
            var permissionId = pr.permissionId();
            LOGGER.info("Timing permission request {} out", permissionId);
            outbox.commit(new EsSimpleEvent(permissionId, PermissionProcessStatus.TIMED_OUT));
        }
    }
}
