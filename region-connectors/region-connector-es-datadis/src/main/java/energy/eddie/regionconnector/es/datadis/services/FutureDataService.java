package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

@Service
public class FutureDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FutureDataService.class);
    private final PermissionRequestService permissionRequestService;
    private final DataApiService dataApiService;

    public FutureDataService(PermissionRequestService permissionRequestService, DataApiService dataApiService) {
        this.permissionRequestService = permissionRequestService;
        this.dataApiService = dataApiService;
    }

    @Scheduled(cron = "${region-connector.es.datadis.polling:0 0 17 * * *}", zone = "Europe/Madrid")
    public void fetchMeteringData() {
        LOGGER.info("Polling for metering data");
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate yesterday = today.minusDays(1);

        permissionRequestService
                .getAllAcceptedPermissionRequests()
                .filter(permissionRequest -> isActive(permissionRequest, today))
                .forEach(permissionRequest -> fetchMeteringDataForRequest(permissionRequest, yesterday));
    }

    private boolean isActive(EsPermissionRequest permissionRequest, LocalDate today) {
        return permissionRequest.start().isBefore(today);
    }

    private void fetchMeteringDataForRequest(EsPermissionRequest permissionRequest, LocalDate yesterday) {
        LocalDate lastPulledOrStart = permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start());
        LocalDate startDate = lastPulledOrStart.isBefore(yesterday) ? lastPulledOrStart : yesterday;

        dataApiService.fetchDataForPermissionRequest(permissionRequest, startDate, yesterday);
    }
}
