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
import java.util.List;

@Service
public class CommonFutureDataService {

    //TODO check out different time zones with @Scheduled annotation

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonFutureDataService.class);

    private final CommonPollingService pollingService;
    //    private final AccountingPointDataService accountingPointDataService;
    private final CommonPermissionRequestRepository repository;
    private final String ZONE;
    private final DataNeedsService dataNeedsService;

    //Finland
    public CommonFutureDataService(
            CommonPollingService pollingService,
            CommonPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService
    ) {
        this.pollingService = pollingService;
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
        this.ZONE = "Europe/Oslo";
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
            pollingService.pollTimeSeriesData((CommonPermissionRequest) activePermission);
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
                pollingService.pollTimeSeriesData((CommonPermissionRequest) activePermission);
            } else {
                LOGGER.atInfo()
                        .addArgument(activePermission::permissionId)
                        .addArgument(dataNeedId)
                        .log("Cannot fetch validated historical data for permission request {}, since it's not the correct data need {}");
            }
        }
    }


    // France


//    public CommonFutureDataService(
//            CommonPollingService pollingService,
//            AccountingPointDataService accountingPointDataService,
//            CommonPermissionRequestRepository repository
//    ) {
//        this.pollingService = pollingService;
//        this.accountingPointDataService = accountingPointDataService;
//        this.repository = repository;
//        this.dataNeedsService = null;
//        this.dataApiService = null;
//        ZONE = "Europe/Paris";
//    }

//    @SuppressWarnings("java:S6857") // Sonar thinks this is malformed, but it's not
//    @Scheduled(cron = "${region-connector.fr.enedis.polling:0 0 17 * * *}", zone = "Europe/Paris")
//    public void fetchMeterReadings() {
//        List<FrEnedisPermissionRequest> acceptedPermissionRequests = repository
//                .findAllByStatus(PermissionProcessStatus.ACCEPTED);
//
//        if (acceptedPermissionRequests.isEmpty()) {
//            LOGGER.info("Found no permission requests to fetch meter readings for");
//            return;
//        }
//
//        LocalDate today = LocalDate.now(ZoneId.of(ZONE));
//        LOGGER.info("Trying to fetch meter readings for {} permission requests", acceptedPermissionRequests.size());
//        for (FrEnedisPermissionRequest acceptedPermissionRequest : acceptedPermissionRequests) {
//            if (acceptedPermissionRequest.granularity() == null) {
//                accountingPointDataService.fetchAccountingPointData(acceptedPermissionRequest,
//                        acceptedPermissionRequest.usagePointId());
//            } else if (isActiveAndNeedsToBeFetched(acceptedPermissionRequest, today)) {
//                fetchMeteringDataForRequest(acceptedPermissionRequest, today);
//            } else {
//                var permissionId = acceptedPermissionRequest.permissionId();
//                LOGGER.info("Permission request {} is not active or data is already up to date", permissionId);
//            }
//        }
//    }
//
//    private boolean isActiveAndNeedsToBeFetched(FrEnedisPermissionRequest permissionRequest, LocalDate today) {
//        return permissionRequest.start().isBefore(today)
//                && permissionRequest.latestMeterReadingEndDate().map(latest -> latest.isBefore(today)).orElse(true);
//    }
//
//    private void fetchMeteringDataForRequest(FrEnedisPermissionRequest permissionRequest, LocalDate today) {
//        LocalDate lastPulledOrStart = permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start());
//
//        pollingService.fetchMeterReadings(permissionRequest,
//                lastPulledOrStart,
//                today);
//    }

    //Spain
//    private final EsPermissionRequestRepository repository;
//    private final CommonDataApiService dataApiService;

//    public CommonFutureDataService(CommonPermissionRequestRepository repository, CommonDataApiService dataApiService) {
//        this.repository = repository;
//        this.dataApiService = dataApiService;
////        this.dataNeedsService = null;
////        this.pollingService = null;
//        ZONE = "Europe/Madrid";
//    }

//    @Scheduled(cron = "${region-connector.es.datadis.polling:0 0 17 * * *}", zone = "Europe/Madrid")
//    public void fetchMeteringData() {
//        LOGGER.info("Polling for metering data");
//        LocalDate today = LocalDate.now(ZoneId.of(ZONE));
//        LocalDate yesterday = today.minusDays(1);
//
//        var acceptedPermissionRequests = repository.findByStatus(PermissionProcessStatus.ACCEPTED);
//        for (EsPermissionRequest permissionRequest : acceptedPermissionRequests) {
//            if (isActive(permissionRequest, today)) {
//                fetchMeteringDataForRequest(permissionRequest, yesterday);
//            }
//        }
//    }
//
//    private boolean isActive(EsPermissionRequest permissionRequest, LocalDate today) {
//        return permissionRequest.start().isBefore(today);
//    }
//
//    private void fetchMeteringDataForRequest(EsPermissionRequest permissionRequest, LocalDate yesterday) {
//        LocalDate lastPulledOrStart = permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start());
//        LocalDate startDate = lastPulledOrStart.isBefore(yesterday) ? lastPulledOrStart : yesterday;
//
//        dataApiService.fetchDataForPermissionRequest(permissionRequest, startDate, yesterday);
//    }




}
