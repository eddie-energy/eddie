package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

@Service
public class HistoricalDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalDataService.class);
    private final PollingService pollingService;

    public HistoricalDataService(PollingService pollingService) {
        this.pollingService = pollingService;
    }

    @Async
    public void fetchHistoricalMeterReadings(FrEnedisPermissionRequest permissionRequest, String usagePointId) {
        LocalDate permissionStart = permissionRequest.start();
        LocalDate permissionEnd = permissionRequest.end();

        LocalDate now = LocalDate.now(ZONE_ID_FR);
        String permissionId = permissionRequest.permissionId();
        if (!permissionStart.isBefore(now)) {
            LOGGER.info("Permission request '{}' is not yet active, skipping data fetch", permissionId);
            return;
        }

        var end = now.isAfter(permissionEnd) ? permissionEnd.plusDays(1) : now;
        pollingService.fetchMeterReadings(permissionRequest, permissionStart, end, usagePointId);
    }
}
