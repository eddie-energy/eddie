package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.shared.utils.MeterReadingEndDate;

import java.time.LocalDate;

/**
 * Record that pairs a permission request with its corresponding validated historical data payload.
 * Used to track data through the processing pipeline from API response to CIM document generation.
 *
 * @param permissionRequest the permission request this data belongs to
 * @param payload           the raw validated historical data from ETA Plus
 */
public record IdentifiableValidatedHistoricalData(
        DePermissionRequest permissionRequest,
        EtaPlusMeteredData payload
) implements IdentifiablePayload<DePermissionRequest, EtaPlusMeteredData>, MeterReadingEndDate {

    @Override
    public LocalDate meterReadingEndDate() {
        return payload.endDate();
    }
}

