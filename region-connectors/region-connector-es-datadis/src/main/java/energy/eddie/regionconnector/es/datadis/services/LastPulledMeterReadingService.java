package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class LastPulledMeterReadingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LastPulledMeterReadingService.class);

    private static boolean isLatestMeterReading(EsPermissionRequest permissionRequest, ZonedDateTime meteringDataDate) {
        return permissionRequest
                .lastPulledMeterReading()
                .map(meteringDataDate::isAfter)
                .orElse(true);
    }

    /**
     * Updates the last pulled meter reading if the last pulled meter reading is older than the metering data end date.
     *
     * @param permissionRequest   the permission request
     * @param meteringDataEndDate the end date of the metering data
     * @return true if the last pulled meter reading was updated
     */
    public boolean updateLastPulledMeterReading(EsPermissionRequest permissionRequest, ZonedDateTime meteringDataEndDate) {
        if (isLatestMeterReading(permissionRequest, meteringDataEndDate)) {
            LOGGER.atInfo()
                    .addArgument(permissionRequest::permissionId)
                    .addArgument(permissionRequest::lastPulledMeterReading)
                    .addArgument(meteringDataEndDate)
                    .log("Updating latest meter reading for permission request {} from {} to {}");
            permissionRequest.setLastPulledMeterReading(meteringDataEndDate);
            return true;
        }

        return false;
    }
}
