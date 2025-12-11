package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.TimeZone;

/**
 * The intent of this class is to abstract the Future Data polling procedure of each individual Region Connector.
 *
 * @param <T> The region connectors PermissionRequest
 */
public class CommonFutureDataService<T extends MeterReadingPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonFutureDataService.class);
    private final CommonPollingService<T> pollingService;
    private final StatusPermissionRequestRepository<T> repository;
    private final RegionConnectorMetadata metadata;
    private final DataNeedCalculationService<DataNeed> calculationService;

    @SuppressWarnings("FutureReturnValueIgnored")
    public CommonFutureDataService(
            CommonPollingService<T> pollingService,
            StatusPermissionRequestRepository<T> repository,
            String cronExpression,
            RegionConnectorMetadata metadata,
            TaskScheduler taskScheduler,
            DataNeedCalculationService<DataNeed> calculationService
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        this.metadata = metadata;
        this.calculationService = calculationService;

        taskScheduler.schedule(this::fetchMeterData,
                               new CronTrigger(cronExpression, TimeZone.getTimeZone(metadata.timeZone())));
    }

    /**
     * This method is used to fetch Future Data for a Region Connector.
     * All active PermissionRequests are fetched from the StatusPermissionRequestRepository.
     * The status of each PermissionRequest is verified, and if needed, polling of FutureData takes place.
     */
    public void fetchMeterData() {
        LOGGER.atInfo()
              .addArgument(metadata::id)
              .log("{}: Polling future data");
        var activePermissions = repository.findByStatus(PermissionProcessStatus.ACCEPTED);
        for (var activePermission : activePermissions) {
            if (!(calculationService.calculate(activePermission.dataNeedId()) instanceof ValidatedHistoricalDataDataNeedResult)) {
                LOGGER.atInfo()
                      .addArgument(metadata::id)
                      .addArgument(activePermission::permissionId)
                      .log("{}: Cannot fetch validated historical data for permission request {}, since it's not the correct data need");
            } else if (pollingService.isActiveAndNeedsToBeFetched(activePermission)) {
                LOGGER.atInfo()
                      .addArgument(metadata::id)
                      .addArgument(activePermission::permissionId)
                      .log("{}: Fetching energy data for permission request {}");
                pollingService.pollTimeSeriesData(activePermission);
            } else {
                LOGGER.atInfo()
                      .addArgument(metadata::id)
                      .addArgument(activePermission::permissionId)
                      .log("{}: permission request {} not active or does not need to be fetched");
            }
        }
    }
}
