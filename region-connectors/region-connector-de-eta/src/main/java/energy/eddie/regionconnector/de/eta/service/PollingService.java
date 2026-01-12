package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Service for polling validated historical data from the ETA Plus API.
 * Implements CommonPollingService to integrate with the CommonFutureDataService
 * for periodic future data polling.
 */
@Service
public class PollingService implements CommonPollingService<DePermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);

    private final DataNeedsService dataNeedsService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public PollingService(DataNeedsService dataNeedsService) {
        this.dataNeedsService = dataNeedsService;
    }

    @Override
    public void pollTimeSeriesData(DePermissionRequest permissionRequest) {
        if (isInactive(permissionRequest)) {
            return;
        }
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Polling validated historical data for permission request {}", permissionId);

        // TODO: Implement actual API call to ETA Plus API
        // The implementation should:
        // 1. Calculate the date range to fetch (from latestMeterReadingEndDate or start to yesterday)
        // 2. Call the ETA Plus API to fetch meter readings
        // 3. Map the response to CIM documents
        // 4. Publish the data via ValidatedHistoricalDataStream
        // 5. Update the latestMeterReadingEndDate on the permission request

        LOGGER.warn("ETA Plus API integration not yet implemented for permission request {}", permissionId);
    }

    @Override
    public boolean isActiveAndNeedsToBeFetched(DePermissionRequest permissionRequest) {
        if (isInactive(permissionRequest)) {
            return false;
        }
        if (permissionRequest.status() != PermissionProcessStatus.ACCEPTED) {
            return false;
        }
        var dataNeedId = permissionRequest.dataNeedId();
        var dataNeed = dataNeedsService.getById(dataNeedId);
        return dataNeed instanceof ValidatedHistoricalDataDataNeed;
    }

    /**
     * Checks if a permission request is currently inactive.
     * A permission request is inactive if its start date is in the future.
     *
     * @param permissionRequest the permission request to check
     * @return true if inactive, false otherwise
     */
    private static boolean isInactive(DePermissionRequest permissionRequest) {
        var now = LocalDate.now(ZoneOffset.UTC);
        return !permissionRequest.start().isBefore(now);
    }
}

