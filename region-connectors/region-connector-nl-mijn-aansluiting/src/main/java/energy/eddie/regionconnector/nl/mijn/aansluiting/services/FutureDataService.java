package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
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
    private final DataNeedsService dataNeedsService;

    public FutureDataService(
            PollingService pollingService,
            NlPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
    }

    @Scheduled(cron = "${region-connector.nl.mijn-aansluting.polling:0 0 17 * * *}", zone = "Europe/Amsterdam")
    public void scheduleNextMeterReading() {
        var activePermissions = repository.findByStatus(PermissionProcessStatus.ACCEPTED);
        for (NlPermissionRequest activePermission : activePermissions) {
            var dataNeedId = activePermission.dataNeedId();
            var dataNeed = dataNeedsService.getById(dataNeedId);
            if (dataNeed instanceof ValidatedHistoricalDataDataNeed) {
                LOGGER.atInfo()
                      .addArgument(activePermission::permissionId)
                      .log("Fetching energy data for permission request {}");
                pollingService.fetchConsumptionData(activePermission);
            } else {
                LOGGER.atInfo()
                      .addArgument(activePermission::permissionId)
                      .addArgument(dataNeedId)
                      .log("Cannot fetch validated historical data for permission request {}, since it's not the correct data need {}");
            }
        }
    }
}
