package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.TimeZone;


public class CommonFutureDataService<T extends PermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonFutureDataService.class);

    private final CommonPollingService<T> pollingService;
    private final StatusPermissionRequestRepository<T> repository;
    private final String timeZone;
    private final DataNeedsService dataNeedsService;
    private final CommonDataApiService<T> dataApiService;
    private final RegionConnector regionConnector;

    public CommonFutureDataService(
            CommonPollingService<T> pollingService,
            StatusPermissionRequestRepository<T> repository,
            DataNeedsService dataNeedsService,
            CommonDataApiService<T> dataApiService,
            String timeZone,
            String cronExpression,
            RegionConnector regionConnector
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
        this.dataApiService = dataApiService;
        this.timeZone = timeZone;
        this.regionConnector = regionConnector;

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        taskScheduler.schedule(this::fetchMeterData, new CronTrigger(cronExpression, TimeZone.getTimeZone(timeZone)));
    }

    public void fetchMeterData() {
        var activePermissions = repository.findByStatus(PermissionProcessStatus.ACCEPTED);
        for (var activePermission : activePermissions) {
            if (dataNeedsService != null) {
                var dataNeedId = activePermission.dataNeedId();
                var dataNeed = dataNeedsService.getById(dataNeedId);
                if (dataNeed instanceof ValidatedHistoricalDataDataNeed) {
                    logPollingStart(activePermission);
                    pollingService.pollTimeSeriesData(activePermission);
                } else {
                    LOGGER.atInfo()
                            .addArgument(regionConnector.getMetadata().id())
                            .addArgument(activePermission::permissionId)
                            .addArgument(dataNeedId)
                            .log("{}: Cannot fetch validated historical data for permission request {}, since it's not the correct data need {}");
                }

            } else {
                if (pollingService != null) {
                    logPollingStart(activePermission);
                    pollingService.pollTimeSeriesData(activePermission);
                } else {
                    logPollingStart(activePermission);
                    dataApiService.pollTimeSeriesData(activePermission, timeZone);
                }
            }

        }
    }

    private void logPollingStart(T activePermission) {
        LOGGER.atInfo()
                .addArgument(regionConnector.getMetadata().id())
                .addArgument(activePermission::permissionId)
                .log("{}: Fetching energy data for permission request {}");
    }

}
