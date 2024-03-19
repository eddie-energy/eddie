package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static energy.eddie.regionconnector.fr.enedis.services.PollingService.MAXIMUM_PERMISSION_DURATION;

@Service
public class HistoricalDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalDataService.class);
    private final PollingService pollingService;

    public HistoricalDataService(PollingService pollingService) {
        this.pollingService = pollingService;
    }

    @Async
    public void fetchHistoricalMeterReadings(FrEnedisPermissionRequest permissionRequest) {
        LocalDate permissionStart = permissionRequest.start().toLocalDate();
        LocalDate permissionEnd = Optional.ofNullable(permissionRequest.end())
                .map(ZonedDateTime::toLocalDate)
                .orElse(permissionStart.plusYears(MAXIMUM_PERMISSION_DURATION));

        LocalDate now = LocalDate.now(ZONE_ID_FR);
        String permissionId = permissionRequest.permissionId();
        if (!permissionStart.isBefore(now)) {
            LOGGER.info("Permission request '{}' is not yet active, skipping data fetch", permissionId);
            return;
        }

        var end = now.isAfter(permissionEnd) ? permissionEnd.plusDays(1) : now;
        pollingService.fetchMeterReadings(permissionRequest, permissionStart, end);
    }
}
