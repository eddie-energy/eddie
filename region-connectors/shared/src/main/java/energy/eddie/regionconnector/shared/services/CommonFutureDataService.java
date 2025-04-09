package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.TimeZone;

/**
 * The intent of this class is to abstract the Future Data polling procedure of each individual Region Connector.
 * @param <T> The region connectors PermissionRequest
 */
public class CommonFutureDataService<T extends MeterReadingPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonFutureDataService.class);
    private final CommonPollingService<T> pollingService;
    private final StatusPermissionRequestRepository<T> repository;
    private final RegionConnectorMetadata metadata;

    public CommonFutureDataService(
            CommonPollingService<T> pollingService,
            StatusPermissionRequestRepository<T> repository,
            String cronExpression,
            RegionConnectorMetadata metadata
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        this.metadata = metadata;

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        taskScheduler.schedule(this::fetchMeterData, new CronTrigger(cronExpression, TimeZone.getTimeZone(metadata.timeZone())));
    }

    /**
     * This method is used to fetch Future Data for a Region Connector.
     * All active PermissionRequests are fetched from the StatusPermissionRequestRepository.
     * The status of each PermissionRequest is verified and if needed, polling of FutureData takes place.
     */
    public void fetchMeterData() {
        var activePermissions = repository.findByStatus(PermissionProcessStatus.ACCEPTED);
        for (var activePermission : activePermissions) {
            if (pollingService.isActiveAndNeedsToBeFetched(activePermission)) {
                LOGGER.atInfo()
                        .addArgument(metadata::id)
                        .addArgument(activePermission::permissionId)
                        .log("{}: Fetching energy data for permission request {}");
                pollingService.pollTimeSeriesData(activePermission);
            } else {
                LOGGER.atInfo()
                        .addArgument(metadata::id)
                        .addArgument(activePermission::permissionId)
                        .log("{}: Cannot fetch validated historical data for permission request {}, since it's not the correct data need");
            }
        }
    }

}
