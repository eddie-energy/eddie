package energy.eddie.regionconnector.at.eda.dto;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.shared.utils.MeterReadingEndDate;

import java.time.LocalDate;
import java.util.List;

public record IdentifiableConsumptionRecord(
        EdaConsumptionRecord consumptionRecord,
        List<AtPermissionRequest> permissionRequests,
        LocalDate meterReadingStartDate,
        LocalDate meterReadingEndDate
) implements MeterReadingEndDate {
}
