package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FutureDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FutureDataService.class);
    private final PollingService pollingService;
    private final NlPermissionRequestRepository repository;

    public FutureDataService(PollingService pollingService, NlPermissionRequestRepository repository) {
        this.pollingService = pollingService;
        this.repository = repository;
    }

    @SuppressWarnings("java:S6857") // Sonar cannot handle this syntax
    @Scheduled(cron = "${region-connector.nl.mijn-aansluting.polling:0 0 17 * * *}", zone = "Europe/Amsterdam")
    public void scheduleNextMeterReading() {
        var activePermissions = repository.findByStatus(PermissionProcessStatus.ACCEPTED);
        for (NlPermissionRequest activePermission : activePermissions) {
            LOGGER.atInfo()
                  .addArgument(activePermission::permissionId)
                  .log("Fetching energy data for permission request {}");
            pollingService.fetchConsumptionData(activePermission);
        }
    }
}
