package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FutureDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FutureDataService.class);
    private final PollingService pollingService;
    private final FiPermissionRequestRepository repository;
    private final DataNeedsService dataNeedsService;

    public FutureDataService(
            PollingService pollingService,
            FiPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
    }

    @Scheduled(cron = "${region-connector.fi.fingrid.polling:0 0 17 * * *}", zone = "Europe/Oslo")
    public void schedulePolling() {
        var activePermissions = repository.findByStatus(PermissionProcessStatus.ACCEPTED);
        for (var activePermission : activePermissions) {
            var dataNeedId = activePermission.dataNeedId();
            var dataNeed = dataNeedsService.getById(dataNeedId);
            if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed)) {
                continue;
            }
            LOGGER.atInfo()
                  .addArgument(activePermission::permissionId)
                  .log("Fetching energy data for permission request {}");
            pollingService.pollTimeSeriesData(activePermission);
        }
    }
}
