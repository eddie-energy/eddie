package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.permission.events.UsStartPollingEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PollingRetryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingRetryService.class);
    private final UsPermissionRequestRepository repository;
    private final Outbox outbox;

    public PollingRetryService(UsPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    @Scheduled(cron = "${region-connector.us.green-button.data-ready.polling:0 0 * * * *}")
    public void polling() {
        var prs = repository.findAllAcceptedAndNotPolled();
        LOGGER.info("Retrying to poll data for {} permission requests", prs.size());
        for (var pr : prs) {
            var permissionId = pr.permissionId();
            LOGGER.info("Retrying to poll data for {}", permissionId);
            outbox.commit(new UsStartPollingEvent(permissionId));
        }
    }
}
