package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutService.class);
    private final EnedisConfiguration configuration;
    private final PermissionRequestService permissionRequestService;

    public TimeoutService(EnedisConfiguration configuration, PermissionRequestService permissionRequestService) {
        this.configuration = configuration;
        this.permissionRequestService = permissionRequestService;
    }

    @SuppressWarnings("java:S6857") // Sonar thinks that the cron placeholder is malformed
    @Scheduled(cron = "${region-connector.fr.enedis.timeout.schedule:0 0 * * * *}")
    public void timeoutPendingPermissionRequests() {
        LOGGER.info("Querying permission requests to timeout.");
        var permissionRequests = permissionRequestService.findTimedOutPermissionRequests(
                configuration.timeoutDuration());
        LOGGER.info("Found {} permission requests to timeout", permissionRequests.size());
        for (var pr : permissionRequests) {
            try {
                pr.receivedPermissionAdministratorResponse();
                pr.timeOut();
            } catch (StateTransitionException e) {
                LOGGER.warn("Got a state transition exception, while time outing permission request", e);
            }
        }
    }
}
