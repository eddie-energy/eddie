package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class LastPulledMeterReadingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LastPulledMeterReadingService.class);

    /**
     * Updates the last pulled meter reading if the last pulled meter reading is older than the metering data end date.
     *
     * @param permissionRequest   the permission request
     * @param meteringDataEndDate the end date of the metering data
     * @return true if the last pulled meter reading was updated
     */
    public boolean updateLastPulledMeterReading(EsPermissionRequest permissionRequest, LocalDate meteringDataEndDate) {
        if (isLatestMeterReading(permissionRequest, meteringDataEndDate)) {
            LOGGER.atInfo()
                  .addArgument(permissionRequest::permissionId)
                  .addArgument(permissionRequest::latestMeterReadingEndDate)
                  .addArgument(meteringDataEndDate)
                  .log("Updating latest meter reading for permission request {} from {} to {}");
            permissionRequest.updateLatestMeterReadingEndDate(meteringDataEndDate);
            return true;
        }

        return false;
    }

    private static boolean isLatestMeterReading(EsPermissionRequest permissionRequest, LocalDate meteringDataDate) {
        return permissionRequest
                .latestMeterReadingEndDate()
                .map(meteringDataDate::isAfter)
                .orElse(true);
    }
}
