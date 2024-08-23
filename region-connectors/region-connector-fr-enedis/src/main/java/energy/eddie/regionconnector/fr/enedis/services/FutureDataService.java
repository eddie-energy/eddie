package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

@Service
public class FutureDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FutureDataService.class);

    private final PollingService pollingService;
    private final AccountingPointDataService accountingPointDataService;
    private final FrPermissionRequestRepository repository;

    public FutureDataService(
            PollingService pollingService,
            AccountingPointDataService accountingPointDataService,
            FrPermissionRequestRepository repository
    ) {
        this.pollingService = pollingService;
        this.accountingPointDataService = accountingPointDataService;
        this.repository = repository;
    }

    @SuppressWarnings("java:S6857") // Sonar thinks this is malformed, but it's not
    @Scheduled(cron = "${region-connector.fr.enedis.polling:0 0 17 * * *}", zone = "Europe/Paris")
    public void fetchMeterReadings() {
        List<FrEnedisPermissionRequest> acceptedPermissionRequests = repository
                .findAllByStatus(PermissionProcessStatus.ACCEPTED);

        if (acceptedPermissionRequests.isEmpty()) {
            LOGGER.info("Found no permission requests to fetch meter readings for");
            return;
        }

        LocalDate today = LocalDate.now(ZONE_ID_FR);
        LOGGER.info("Trying to fetch meter readings for {} permission requests", acceptedPermissionRequests.size());
        for (FrEnedisPermissionRequest acceptedPermissionRequest : acceptedPermissionRequests) {
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

    private boolean isActiveAndNeedsToBeFetched(FrEnedisPermissionRequest permissionRequest, LocalDate today) {
        return permissionRequest.start().isBefore(today)
               && permissionRequest.latestMeterReadingEndDate().map(latest -> latest.isBefore(today)).orElse(true);
    }

    private void fetchMeteringDataForRequest(FrEnedisPermissionRequest permissionRequest, LocalDate today) {
        LocalDate lastPulledOrStart = permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start());

        pollingService.fetchMeterReadings(permissionRequest,
                                          lastPulledOrStart,
                                          today);
    }
}
