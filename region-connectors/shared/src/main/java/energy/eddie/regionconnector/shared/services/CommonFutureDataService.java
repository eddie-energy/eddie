package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class CommonFutureDataService {

    //TODO check out different time zones with @Scheduled annotation

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonFutureDataService.class);

    private final CommonPollingService pollingService;
    private final CommonPermissionRequestRepository repository;
    private final String ZONE;
    private final DataNeedsService dataNeedsService;
    private final CommonDataApiService dataApiService;
    private final CommonAccountingPointDataService accountingPointDataService;

    //Finland
    public CommonFutureDataService(
            CommonPollingService pollingService,
            CommonPermissionRequestRepository repository,
            DataNeedsService dataNeedsService,
            CommonDataApiService dataApiService,
            CommonAccountingPointDataService accountingPointDataService

    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
        this.dataApiService = dataApiService;
        this.accountingPointDataService = accountingPointDataService;
        this.ZONE = "Europe/Paris";
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

    @Scheduled(cron = "${region-connector.nl.mijn-aansluting.polling:0 0 17 * * *}", zone = "Europe/Amsterdam")
    public void scheduleNextMeterReading() {
        var activePermissions = repository.findByStatus(PermissionProcessStatus.ACCEPTED);
        for (var activePermission : activePermissions) {
            var dataNeedId = activePermission.dataNeedId();
            var dataNeed = dataNeedsService.getById(dataNeedId);
            if (dataNeed instanceof ValidatedHistoricalDataDataNeed) {
                LOGGER.atInfo()
                        .addArgument(activePermission::permissionId)
                        .log("Fetching energy data for permission request {}");
                pollingService.pollTimeSeriesData(activePermission);
            } else {
                LOGGER.atInfo()
                        .addArgument(activePermission::permissionId)
                        .addArgument(dataNeedId)
                        .log("Cannot fetch validated historical data for permission request {}, since it's not the correct data need {}");
            }
        }
    }

    @Scheduled(cron = "${region-connector.es.datadis.polling:0 0 17 * * *}", zone = "Europe/Madrid")
    public void fetchMeteringData() {
        LOGGER.info("Polling for metering data");
        LocalDate today = LocalDate.now(ZoneId.of(ZONE));
        LocalDate yesterday = today.minusDays(1);

        var acceptedPermissionRequests = repository.findByStatus(PermissionProcessStatus.ACCEPTED);
        for (var permissionRequest : acceptedPermissionRequests) {
            if (isActive(permissionRequest, today)) {
                fetchMeteringDataForRequest(permissionRequest, yesterday);
            }
        }
    }

    private boolean isActive(CommonPermissionRequest permissionRequest, LocalDate today) {
        return permissionRequest.start().isBefore(today);
    }

    private void fetchMeteringDataForRequest(CommonPermissionRequest permissionRequest, LocalDate yesterday) {
        LocalDate lastPulledOrStart = permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start());
        LocalDate startDate = lastPulledOrStart.isBefore(yesterday) ? lastPulledOrStart : yesterday;

        if (ZONE.equals("Europe/Paris")){
            pollingService.fetchMeterReadings(permissionRequest,
                    lastPulledOrStart,
                    yesterday);
        } else {

            dataApiService.fetchDataForPermissionRequest(permissionRequest, startDate, yesterday);
        }
    }

    // France
    @SuppressWarnings("java:S6857") // Sonar thinks this is malformed, but it's not
    @Scheduled(cron = "${region-connector.fr.enedis.polling:0 0 17 * * *}", zone = "Europe/Paris")
    public void fetchMeterReadings() {
        var acceptedPermissionRequests = repository
                .findByStatus(PermissionProcessStatus.ACCEPTED);

        if (acceptedPermissionRequests.isEmpty()) {
            LOGGER.info("Found no permission requests to fetch meter readings for");
            return;
        }

        LocalDate today = LocalDate.now(ZoneId.of(ZONE));
        LOGGER.info("Trying to fetch meter readings for {} permission requests", acceptedPermissionRequests.size());
        for (var acceptedPermissionRequest : acceptedPermissionRequests) {
            if (acceptedPermissionRequest.granularity() == null) {
                accountingPointDataService.fetchAccountingPointData(acceptedPermissionRequest,
                        acceptedPermissionRequest.usagePointId());
            } else if (isActiveAndNeedsToBeFetched(acceptedPermissionRequest, today)) {
                fetchMeteringDataForRequest(acceptedPermissionRequest, today);
            } else {
                var permissionId = acceptedPermissionRequest.permissionId();
                LOGGER.info("Permission request {} is not active or data is already up to date", permissionId);
            }
        }
    }

    private boolean isActiveAndNeedsToBeFetched(CommonPermissionRequest permissionRequest, LocalDate today) {
        return permissionRequest.start().isBefore(today)
                && permissionRequest.latestMeterReadingEndDate().map(latest -> latest.isBefore(today)).orElse(true);
    }

}
