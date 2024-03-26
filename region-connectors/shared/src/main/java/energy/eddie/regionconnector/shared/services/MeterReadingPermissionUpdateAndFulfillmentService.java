package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.regionconnector.shared.utils.MeterReadingEndDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class MeterReadingPermissionUpdateAndFulfillmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MeterReadingPermissionUpdateAndFulfillmentService.class);

    private final FulfillmentService fulfillmentService;

    public MeterReadingPermissionUpdateAndFulfillmentService(
            FulfillmentService fulfillmentService
    ) {
        this.fulfillmentService = fulfillmentService;
    }

    /**
     * Tries to update the latest meter reading end date and fulfill the permission request if applicable.
     *
     * @param permissionRequest   the permission request
     * @param meterReadingEndDate the meter reading end date
     */
    public void tryUpdateAndFulfillPermissionRequest(
            MeterReadingPermissionRequest permissionRequest,
            MeterReadingEndDate meterReadingEndDate
    ) {
        LocalDate endDate = meterReadingEndDate.meterReadingEndDate();
        if (updateLatestMeterReadingEndDate(permissionRequest, endDate)
                && fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, endDate)) {
            fulfillmentService.tryFulfillPermissionRequest(permissionRequest);
        }
    }


    /**
     * Updates the latest meter reading end date if the given metering data end date is newer.
     *
     * @param permissionRequest   the permission request
     * @param meterReadingEndDate the end date of the meter reading
     * @return true if the latest meter reading end date was updated
     */
    private boolean updateLatestMeterReadingEndDate(
            MeterReadingPermissionRequest permissionRequest,
            LocalDate meterReadingEndDate
    ) {
        if (needsToBeUpdated(permissionRequest, meterReadingEndDate)) {
            LOGGER.atInfo()
                  .addArgument(() -> permissionRequest.dataSourceInformation().regionConnectorId())
                  .addArgument(permissionRequest::permissionId)
                  .addArgument(permissionRequest::latestMeterReadingEndDate)
                  .addArgument(meterReadingEndDate)
                  .log("{}: Updating latest meter reading for permission request {} from {} to {}");
            permissionRequest.updateLatestMeterReadingEndDate(meterReadingEndDate);
            return true;
        }

        LOGGER.atDebug()
              .addArgument(() -> permissionRequest.dataSourceInformation().regionConnectorId())
              .addArgument(permissionRequest::permissionId)
              .addArgument(permissionRequest::latestMeterReadingEndDate)
              .addArgument(meterReadingEndDate)
              .log("{}: Not updating latest meter reading for permission request {} from {} to {}");
        return false;
    }

    private static boolean needsToBeUpdated(
            MeterReadingPermissionRequest permissionRequest,
            LocalDate meterReadingEndDataDate
    ) {
        return permissionRequest
                .latestMeterReadingEndDate()
                .map(meterReadingEndDataDate::isAfter)
                .orElse(true);
    }
}
